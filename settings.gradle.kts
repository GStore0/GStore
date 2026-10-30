/*
 * SPDX-FileCopyrightText: 2021-2025 Rahul Kumar Patel <whyorean@gmail.com>
 * SPDX-FileCopyrightText: 2022-2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // libsu is only available via jitpack
        maven("https://jitpack.io/") {
            content {
                includeModule("com.github.topjohnwu.libsu", "core")
            }
        }
        // Only included in huawei variants
        maven("https://developer.huawei.com/repo/") {
            content {
                includeGroup("com.huawei.hms")
                includeGroup("com.huawei.android.hms")
            }
        }

        maven { url = uri("https://jitpack.io") }

        maven {
            // A repository must be specified for some reason. "registry" is a dummy.
            url = uri("https://maven.pkg.github.com/MorpheApp/registry")
            credentials {
                val hardcodedUser = ""
                val hardcodedToken = ""
                val gprUser: String? = providers.gradleProperty("gpr.user").orNull
                val gprKey: String? = providers.gradleProperty("gpr.key").orNull

                username = (hardcodedUser.ifBlank { System.getenv("GITHUB_ACTOR") ?: gprUser })
                password = (hardcodedToken.ifBlank { System.getenv("GITHUB_TOKEN") ?: gprKey })
            }
        }
    }
}
include(":app")
include(":extension")
include(":extension:stub")
rootProject.name = "GStore"
