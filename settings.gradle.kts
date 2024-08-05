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
        google {
            content {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        mavenLocal {
            content {
                includeGroupAndSubgroups("com.tangem.tangem-sdk-kotlin")
                includeGroupAndSubgroups("com.tangem.vico")
                includeModule("com.tangem", "blstlib")
                includeModule("com.tangem", "blockchain")
                includeModule("com.tangem", "wallet-core-proto")
                includeModule("com.tangem", "wallet-core")
            }
        }
        maven {
            // setting any repository from tangem project allows maven search all packages in the project
            url = uri("https://maven.pkg.github.com/tangem/tangem-sdk-android")
            credentials {
                username = properties.getProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
                password = properties.getProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
            }
            content { includeGroupAndSubgroups("com.tangem.tangem-sdk-kotlin") }
        }
        maven {
            // setting any repository from tangem project allows maven search all packages in the project
            url = uri("https://maven.pkg.github.com/tangem/blst-android")
            credentials {
                username = properties.getProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
                password = properties.getProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
            }
            content { includeModule("com.tangem", "blstlib") }
        }
        maven {
            // setting any repository from tangem project allows maven search all packages in the project
            url = uri("https://maven.pkg.github.com/tangem/blockchain-sdk-kotlin")
            credentials {
                username = properties.getProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
                password = properties.getProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
            }
            content { includeModule("com.tangem", "blockchain") }
        }
        maven {
            // setting any repository from tangem project allows maven search all packages in the project
            url = uri("https://maven.pkg.github.com/tangem/wallet-core")
            credentials {
                username = properties.getProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
                password = properties.getProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
            }
            content {
                includeModule("com.tangem", "wallet-core-proto")
                includeModule("com.tangem", "wallet-core")
            }
        }
        maven {
            // setting any repository from tangem project allows maven search all packages in the project
            url = uri("https://maven.pkg.github.com/tangem/vico")
            credentials {
                username = properties.getProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
                password = properties.getProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
            }
            content {
                includeGroupAndSubgroups("com.tangem.vico")
            }
        }
        jcenter { // unable to replace with mavenCentral() due to rekotlin
            content {
                includeModule("org.rekotlin", "rekotlin")
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
include(":common:ui-charts")
include(":common:routing")

// region Core modules
include(":core:analytics")
include(":core:analytics:models")
include(":core:datasource")
include(":core:featuretoggles")
include(":core:navigation")
include(":core:res")
include(":core:ui")
include(":core:utils")
include(":core:deep-links")
include(":core:deep-links:global")
include(":core:decompose")
include(":core:pagination")
// endregion Core modules

// region Common modules
include(":common:ui")
// endregion

// region Libs modules
include(":libs:auth")
include(":libs:blockchain-sdk")
include(":libs:crypto")
include(":libs:visa")
// endregion Libs modules

// region Feature modules
include(":features:onboarding")

include(":features:referral:data")
include(":features:referral:domain")
include(":features:referral:presentation")

include(":features:swap:api")
include(":features:swap:data")
include(":features:swap:domain")
include(":features:swap:domain:models")
include(":features:swap:domain:api")
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

include(":features:qr-scanning:api")
include(":features:qr-scanning:impl")

include(":features:staking:api")
include(":features:staking:impl")

include(":features:details:api")
include(":features:details:impl")

include(":features:disclaimer:api")
include(":features:disclaimer:impl")

include(":features:push-notifications:api")
include(":features:push-notifications:impl")

include(":features:wallet-settings:api")
include(":features:wallet-settings:impl")

include(":features:markets:api")
include(":features:markets:impl")
// endregion Feature modules

// region Domain modules
// [REDACTED_TODO_COMMENT]
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
include(":domain:transaction:models")
include(":domain:analytics")
include(":domain:visa")
include(":domain:onboarding")
include(":domain:feedback")
include(":domain:qr-scanning")
include(":domain:qr-scanning:models")
include(":domain:staking")
include(":domain:staking:models")
include(":domain:wallet-connect")
include(":domain:markets")
include(":domain:markets:models")
include(":domain:manage-tokens")
include(":domain:manage-tokens:models")
// endregion Domain modules

// region Data modules
include(":data:app-currency")
include(":data:app-theme")
include(":data:balance-hiding")
include(":data:common")
include(":data:card")
include(":data:tokens")
include(":data:settings")
include(":data:txhistory")
include(":data:wallets")
include(":data:analytics")
include(":data:transaction")
include(":data:visa")
include(":data:promo")
include(":data:onboarding")
include(":data:feedback")
include(":data:qr-scanning")
include(":data:staking")
include(":data:wallet-connect")
include(":data:markets")
// endregion Data modules
