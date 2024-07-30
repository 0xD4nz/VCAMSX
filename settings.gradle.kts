pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://plugins.gradle.org/m2/")
        maven("https://maven.aliyun.com/nexus/content/repositories/google")
        maven("https://maven.aliyun.com/nexus/content/groups/public")
        maven("https://maven.aliyun.com/nexus/content/repositories/jcenter")
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://api.xposed.info/")
//        maven ("https://maven.pkg.github.com/GCX-HCI/tray" )
    }
}

rootProject.name = "VCAMSX-EX"
include(":app")
 