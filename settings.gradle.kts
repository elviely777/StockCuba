pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
        maven { url = uri("https://maven.google.com") }
        mavenCentral()
    }
    plugins {
        id("com.android.application") version "8.7.3" apply false
        id("org.jetbrains.kotlin.android") version "2.0.21" apply false
        id("org.jetbrains.kotlin.kapt") version "2.0.21" apply false
        id("com.google.dagger.hilt.android") version "2.52" apply false
        // id("com.google.gms.google-services") version "4.4.2" apply false  // REMOVIDO - migración a Supabase
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}

rootProject.name = "StockCuba"
include(":app")