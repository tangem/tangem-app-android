pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }

    includeBuild("plugins/configuration")
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        jcenter() // unable to replace with mavenCentral() due to rekotlin and com.otaliastudios:cameraview
        maven("https://nexus.tangem-tech.com/repository/maven-releases/")
        maven("https://jitpack.io")
        maven("https://zendesk.jfrog.io/zendesk/repo")
    }

    versionCatalogs {
        create("deps") {
            from(files("gradle/dependencies.toml"))
        }
    }
}

include(":app")
include(":domain")
include(":common")

// region Core modules
include(":core:analytics")
include(":core:datasource")
include(":core:featuretoggles")
include(":core:res")
include(":core:utils")
include(":core:ui")
// endregion Core modules

// region Libs modules
include(":libs:crypto")
include(":libs:auth")
// endregion Libs modules

// region Feature modules
include(":features:referral:data")
include(":features:referral:domain")
include(":features:referral:presentation")
include(":features:swap:data")
include(":features:swap:domain")
include(":features:swap:presentation")

include(":features:tester:api")
include(":features:tester:impl")
// endregion Feature modules
