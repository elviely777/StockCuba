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