// Proxy config for plugin resolution (runs before gradle.properties)
System.setProperty("http.proxyHost", "10.12.0.205")
System.setProperty("http.proxyPort", "3128")
System.setProperty("http.proxyUser", "emorales")
System.setProperty("http.proxyPassword", "Eviel.22")
System.setProperty("https.proxyHost", "10.12.0.205")
System.setProperty("https.proxyPort", "3128")
System.setProperty("https.proxyUser", "emorales")
System.setProperty("https.proxyPassword", "Eviel.22")
System.setProperty("http.nonProxyHosts", "localhost|127.0.0.1|*.local|*xetid.cu")
System.setProperty("https.nonProxyHosts", "localhost|127.0.0.1|*.local|*xetid.cu")

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenLocal()
        mavenCentral()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/kotlin/kotlin-maven/plugins/") }
    }
    plugins {
        id("com.android.application") version "8.7.3" apply false
        id("org.jetbrains.kotlin.android") version "2.0.21" apply false
        id("org.jetbrains.kotlin.kapt") version "2.0.21" apply false
        id("com.google.dagger.hilt.android") version "2.52" apply false
        id("androidx.room") version "2.6.1" apply false
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