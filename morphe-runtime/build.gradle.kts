plugins {
    alias(libs.plugins.android.library)
}

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
}

dependencies {
    // morphe
    implementation("app.morphe:morphe-library:1.4.0:")
    implementation("app.morphe:morphe-patcher:2.4.0")

    compileOnly(libs.androidx.core.ktx)
}