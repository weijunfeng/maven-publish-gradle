pluginManagement {
    repositories {
        maven {
            setUrl("./maven-publish-gradle-plugin/build/repo/")
        }
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        maven {
            setUrl("./maven-publish-gradle-plugin/build/repo/")
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "maven-publish-gradle"
include(":androidApp")
include(":maven-publish-gradle-plugin")
include(":shared")