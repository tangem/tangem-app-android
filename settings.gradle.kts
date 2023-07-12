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
        mavenLocal()
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

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")
include(":common")

// region Core modules
include(":core:analytics")
include(":core:analytics:models")
include(":core:datasource")
include(":core:featuretoggles")
include(":core:navigation")
include(":core:res")
include(":core:ui")
include(":core:utils")
// endregion Core modules

// region Libs modules
include(":libs:crypto")
include(":libs:auth")
// endregion Libs modules

// region Feature modules
include(":features:onboarding")
include(":features:referral:data")
include(":features:referral:domain")
include(":features:referral:presentation")
include(":features:swap:api")
include(":features:swap:data")
include(":features:swap:domain")
include(":features:swap:presentation")

include(":features:tester:api")
include(":features:tester:impl")

include(":features:wallet:api")
include(":features:wallet:impl")

include(":features:tokendetails:api")
include(":features:tokendetails:impl")

include(":features:learn2earn:api")
include(":features:learn2earn:impl")
// endregion Feature modules

// region Domain modules
// TODO: Remove, temporary modules
include(":domain:models")
include(":domain:legacy")

include(":domain:core")
include(":domain:card")
include(":domain:wallets")
include(":domain:wallets:models")
include(":domain:tokens")
// endregion Domain modules

// region Data modules
include(":data:tokens")
include(":data:source:preferences")
// endregion Data modules