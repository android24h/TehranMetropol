pluginManagement {
    repositories {
        // dl.google.com is unreachable from some networks in Iran, so Google-hosted groups
        // (AndroidX and the Android Gradle Plugin) come from the Myket mirror first; google()
        // remains the fallback. Every other group is never requested from the mirror.
        maven("https://maven.myket.ir") {
            content {
                includeGroupByRegex("androidx\\..*")
                includeGroupByRegex("com\\.android(\\..*)?")
            }
        }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // See pluginManagement above.
        maven("https://maven.myket.ir") {
            content {
                includeGroupByRegex("androidx\\..*")
                includeGroupByRegex("com\\.android(\\..*)?")
            }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "Tehran Metropoll"
include(":app")
