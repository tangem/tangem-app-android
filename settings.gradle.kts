pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }

    includeBuild("plugins/configuration")
}

val properties = java.util.Properties()
val propertiesFile = File(rootDir.absolutePath, "local.properties")
if (propertiesFile.exists()) {
    properties.load(propertiesFile.inputStream())
    println("Authenticating user: " + properties.getProperty("gpr.user"))
} else {
    println(
        "local.properties not found, please create it next to build.gradle and set gpr.user and gpr.key (Create a GitHub package read only + non expiration token at https://github.com/settings/tokens)\n" +
            "Or set GITHUB_ACTOR and GITHUB_TOKEN environment variables"
    )
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        mavenLocal()
        jcenter() // unable to replace with mavenCentral() due to rekotlin and com.otaliastudios:cameraview
        maven("https://nexus.tangem-tech.com/repository/maven-releases/")
        maven {
            // setting any repository from tangem project allows maven search all packages in the project
            url = uri("https://maven.pkg.github.com/tangem/blockchain-sdk-kotlin")
            credentials {
                username = properties.getProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
                password = properties.getProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
            }
        }
        maven {
            // setting any repository from tangem project allows maven search all packages in the project
            url = uri("https://maven.pkg.github.com/tangem/wallet-core")
            credentials {
                username = properties.getProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
                password = properties.getProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
            }
        }
        maven("https://jitpack.io")
        maven("https://clients-nexus.sprinklr.com/")
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

include(":features:send:api")
include(":features:send:impl")

include(":features:manage-tokens:api")
include(":features:manage-tokens:impl")
// endregion Feature modules

// region Domain modules
// TODO: Remove, temporary modules
include(":domain:models")
include(":domain:legacy")

include(":domain:card")
include(":domain:core")
include(":domain:demo")
include(":domain:settings")
include(":domain:tokens")
include(":domain:tokens:models")
include(":domain:wallets")
include(":domain:wallets:models")
include(":domain:txhistory")
include(":domain:txhistory:models")
include(":domain:app-currency")
include(":domain:app-currency:models")
include(":domain:app-theme")
include(":domain:app-theme:models")
include(":domain:balance-hiding")
include(":domain:balance-hiding:models")
include(":domain:transaction")
include(":domain:analytics")
// endregion Domain modules

// region Data modules
include(":data:app-currency")
include(":data:app-theme")
include(":data:balance-hiding")
include(":data:common")
include(":data:card")
include(":data:tokens")
include(":data:source:preferences")
include(":data:settings")
include(":data:txhistory")
include(":data:wallets")
include(":data:analytics")
// endregion Data modules