
#include <jni.h>
#include <android/log.h>
#include <android/multinetwork.h>

#include <dlfcn.h>
#include <errno.h>
#include <signal.h>
#include <pthread.h>
#include <netdb.h>
#include <stdint.h>
#include <sys/types.h>
#include <stdarg.h>
#include <sys/syscall.h>
#include <string.h>
#include <stdio.h>
#include <unistd.h>
#include <stdatomic.h>

#include "And64InlineHook.hpp"

#define NETFLIX_PACKAGE_NAME "com.netflix.mediaclient"
#define TAG "NetflixDontKill"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static JavaVM *g_vm = NULL;
static atomic_int g_hooks_installed = 0;

// Original function pointers
static int (*orig_kill)(pid_t, int);

static int (*orig_killpg)(pid_t, int);

static int (*orig_raise)(int);

static int (*orig_pthread_kill)(pthread_t, int);

typedef long (*syscall_fn)(
        long number,
        long arg0,
        long arg1,
        long arg2,
        long arg3,
        long arg4,
        long arg5
);

static syscall_fn orig_syscall = NULL;

// ==========================================
// Kill interceptors
// ==========================================

static int hook_kill(pid_t pid, int sig) {
    LOGI("Blocked kill(pid=%d, sig=%d)", pid, sig);
    errno = EPERM;
    return -1;
}

static int hook_killpg(pid_t pgrp, int sig) {
    LOGI("Blocked killpg(pgrp=%d, sig=%d)", pgrp, sig);
    errno = EPERM;
    return -1;
}

static int hook_raise(int sig) {
    LOGI("Blocked raise(sig=%d)", sig);
    errno = EPERM;
    return -1;
}

static int hook_pthread_kill(pthread_t thread, int sig) {
    LOGI("Blocked pthread_kill(thread=%lu, sig=%d)", (unsigned long) thread, sig);
    return EPERM;
}

static long hook_syscall(
        long number,
        long arg0,
        long arg1,
        long arg2,
        long arg3,
        long arg4,
        long arg5
) {
    switch (number) {
        case SYS_kill:
        case SYS_tkill:
        case SYS_tgkill:
            LOGI("Blocked syscall: %ld", number);
            errno = EPERM;
            return -1;

        default:
            break;
    }

    // Forward all six arguments.
    // This allows futex and other syscalls to work.
    return orig_syscall(number, arg0, arg1, arg2, arg3, arg4, arg5);
}

// ==========================================
// Hook helper
// ==========================================

static void install_hook(
        const char *name,
        void *replacement,
        void **original
) {
    void *target = dlsym(RTLD_DEFAULT, name);

    if (!target) {
        LOGE("Symbol not found: %s", name);
        return;
    }

    A64HookFunction(target, replacement, original);
    LOGI("Hooked %s @ %p", name, target);
}

// ==========================================
// JNI
// ==========================================
static jobject get_current_application(JNIEnv *env) {
    jclass clazz = (*env)->FindClass(
            env, "android/app/ActivityThread"
    );

    if (!clazz) {
        (*env)->ExceptionClear(env);
        return NULL;
    }

    jmethodID method = (*env)->GetStaticMethodID(
            env,
            clazz,
            "currentApplication",
            "()Landroid/app/Application;"
    );

    if (!method) {
        (*env)->ExceptionClear(env);
        return NULL;
    }

    jobject app = (*env)->CallStaticObjectMethod(
            env, clazz, method
    );

    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionClear(env);
        return NULL;
    }

    return app;
}

static int is_netflix_package(JNIEnv *env, jobject app) {
    if (!env || !app) return 0;

    jclass clazz = (*env)->GetObjectClass(env, app);
    if (!clazz) return 0;

    jmethodID get_package = (*env)->GetMethodID(
            env, clazz,
            "getPackageName",
            "()Ljava/lang/String;"
    );

    if (!get_package) {
        (*env)->DeleteLocalRef(env, clazz);
        return 0;
    }

    jstring package = (jstring) (*env)->CallObjectMethod(env, app, get_package);

    if (!package) {
        (*env)->DeleteLocalRef(env, clazz);
        return 0;
    }

    const char *pkg = (*env)->GetStringUTFChars(env, package, NULL);

    int result = pkg && strcmp(pkg, NETFLIX_PACKAGE_NAME) == 0;

    if (pkg) (*env)->ReleaseStringUTFChars(env, package, pkg);

    (*env)->DeleteLocalRef(env, package);
    (*env)->DeleteLocalRef(env, clazz);

    return result;
}

static void install_all_hooks(void) {
    if (atomic_exchange(&g_hooks_installed, 1))
        return;

    LOGI("Installing main process hooks");

    // Install kill hooks
    install_hook("kill",
                 (void *) hook_kill,
                 (void **) &orig_kill
    );

    install_hook("killpg",
                 (void *) hook_killpg,
                 (void **) &orig_killpg
    );

    install_hook("raise",
                 (void *) hook_raise,
                 (void **) &orig_raise
    );

    install_hook("pthread_kill",
                 (void *) hook_pthread_kill,
                 (void **) &orig_pthread_kill
    );

    install_hook(
            "syscall",
            (void *) hook_syscall,
            (void **) &orig_syscall
    );

    LOGI("All hooks installed");
}

static void *watchdog_thread(void *arg) {
    JNIEnv *env = NULL;
    int attached = 0;
    int timeout = 1000000 * 60; // 60s
    int elapsed = 0;

    if ((*g_vm)->GetEnv(
            g_vm, (void **) &env, JNI_VERSION_1_6
    ) != JNI_OK) {

        if ((*g_vm)->AttachCurrentThread(
                g_vm, &env, NULL
        ) != JNI_OK) {
            return NULL;
        }

        attached = 1;
    }

    LOGI("Application watchdog started");

    while (elapsed < timeout) {
        jobject app = get_current_application(env);

        if (app != NULL) {
            LOGI("Application available");

            if (is_netflix_package(env, app)) {
                install_all_hooks();
            } else {
                LOGI("Not Netflix. Hooks skipped.");
            }

            (*env)->DeleteLocalRef(env, app);
            break;
        }

        usleep(100000); // 100 ms
        elapsed += 100000;
    }

    if (attached) (*g_vm)->DetachCurrentThread(g_vm);
    return NULL;
}

JNIEXPORT jint JNICALL JNI_OnLoad(
        JavaVM *vm,
        void *reserved
) {
    g_vm = vm;
    pthread_t thread;
    if (pthread_create(&thread, NULL, watchdog_thread, NULL) == 0) {
        pthread_detach(thread);
        LOGI("Application watchdog created");
    } else {
        LOGE("Failed to create watchdog");
    }

    return JNI_VERSION_1_6;
}