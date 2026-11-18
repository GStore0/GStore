plugins {
    alias(libs.plugins.android.library)
}

val morphePatcherVersionName = "2.4.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
android {
    namespace = "com.kangrio.morphe"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        minSdk {
            version = release(24)
        }
//        targetSdk {
//            version = release(37)
//        }

        buildConfigField("String", "MORPHE_PATCHER_VERSION_NAME", "\"$morphePatcherVersionName\"")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildTypes {
        release {
            isMinifyEnabled = true
//            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    // morphe
    implementation("app.morphe:morphe-library:1.4.0:")
    implementation("app.morphe:morphe-patcher:$morphePatcherVersionName")

    compileOnly(libs.androidx.core.ktx)
}