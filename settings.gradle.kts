pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven { url = uri("https://developer.huawei.com/repo/") }
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
        flatDir {
            dirs("app/libs")
        }
        google {
            content {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        maven { url = uri("https://developer.huawei.com/repo/") }
        mavenLocal {
            content {
                includeGroupAndSubgroups("com.tangem.tangem-sdk-kotlin")
                includeGroupAndSubgroups("com.tangem.tangem-hot-sdk-kotlin")
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
            url = uri("https://maven.pkg.github.com/tangem/tangem-hot-sdk-kotlin")
            credentials {
                username = properties.getProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
                password = properties.getProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
            }
            content { includeGroupAndSubgroups("com.tangem.tangem-hot-sdk-kotlin") }
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
        maven {
            // setting any repository from tangem project allows maven search all packages in the project
            url = uri("https://maven.pkg.github.com/tangem/ic4j-agent")
            credentials {
                username = properties.getProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
                password = properties.getProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
            }
            content {
                includeGroupAndSubgroups("com.tangem.ic4j")
            }
        }
        maven {
            // setting any repository from tangem project allows maven search all packages in the project
            url = uri("https://maven.pkg.github.com/tangem/web3j")
            credentials {
                username = properties.getProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
                password = properties.getProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
            }
            content {
                includeGroupAndSubgroups("org.web3j")
            }
        }
        maven("https://jitpack.io")
        maven("https://maven.sumsub.com/repository/maven-public/")
    }

    versionCatalogs {
        create("deps") {
            from(files("gradle/dependencies.toml"))
        }
        create("tangemDeps") {
            from(files("gradle/tangem_dependencies.toml"))
        }
    }

}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")
include(":plugins:detekt-rules")

// region Core modules
include(":core:analytics")
include(":core:analytics:models")
include(":core:datasource")
include(":core:config-toggles")
include(":core:navigation")
include(":core:res")
include(":core:ui")
include(":core:utils")
include(":core:decompose")
include(":core:pagination")
include(":core:error")
include(":core:error:ext")
// endregion Core modules

// region Common modules
include(":common")
include(":common:google")
include(":common:routing")
include(":common:test")
include(":common:ui")
include(":common:ui-charts")
// endregion

// region Libs modules
include(":libs:auth")
include(":libs:blockchain-sdk")
include(":libs:crypto")
include(":libs:visa")
include(":libs:tangem-sdk-api")
// endregion Libs modules

// region Feature modules
include(":features:onboarding-v2:api")
include(":features:onboarding-v2:impl")

include(":features:home:api")
include(":features:home:impl")

include(":features:referral:api")
include(":features:referral:data")
include(":features:referral:domain")
include(":features:referral:impl")

include(":features:swap:api")
include(":features:swap:data")
include(":features:swap:domain")
include(":features:swap:domain:models")
include(":features:swap:domain:api")
include(":features:swap:impl")
include(":features:swap-v2:api")
include(":features:swap-v2:impl")

include(":features:tester:api")
include(":features:tester:impl")

include(":features:wallet:api")
include(":features:wallet:impl")

include(":features:tokendetails:api")
include(":features:tokendetails:impl")

include(":features:send-v2:api")
include(":features:send-v2:impl")

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

include(":features:usedesk:api")
include(":features:usedesk:impl")

include(":features:push-notifications:api")
include(":features:push-notifications:impl")

include(":features:wallet-settings:api")
include(":features:wallet-settings:impl")

include(":features:markets:api")
include(":features:markets:impl")

include(":features:onramp:api")
include(":features:onramp:impl")

include(":features:stories:api")
include(":features:stories:impl")

include(":features:txhistory:api")
include(":features:txhistory:impl")

include(":features:biometry:api")
include(":features:biometry:impl")

include(":features:nft:api")
include(":features:nft:impl")

include(":features:walletconnect:api")
include(":features:walletconnect:impl")

include(":features:hot-wallet:api")
include(":features:hot-wallet:impl")

include(":features:kyc:api")
//TODO disable for release because of the permissions
// include(":features:kyc:impl")

include(":features:tangempay:main:api")
include(":features:tangempay:main:impl")

include(":features:tangempay:details:api")
include(":features:tangempay:details:impl")

include(":features:tangempay:onboarding:api")
include(":features:tangempay:onboarding:impl")

include(":features:create-wallet-selection:api")
include(":features:create-wallet-selection:impl")

include(":features:welcome:api")
include(":features:welcome:impl")

include(":features:account:api")
include(":features:account:impl")

include(":features:token-recieve:api")
include(":features:token-recieve:impl")

include(":features:yield-supply:api")
include(":features:yield-supply:impl")
// endregion Feature modules

// region Domain modules
// TODO: Remove, temporary modules
include(":domain:models")
include(":domain:legacy")

include(":domain:account")
include(":domain:card")
include(":domain:core")
include(":domain:demo")
include(":domain:demo:models")
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
include(":domain:visa:models")
include(":domain:onboarding")
include(":domain:feedback")
include(":domain:feedback:models")
include(":domain:qr-scanning")
include(":domain:qr-scanning:models")
include(":domain:staking")
include(":domain:staking:models")
include(":domain:wallet-connect")
include(":domain:wallet-connect:models")
include(":domain:markets")
include(":domain:markets:models")
include(":domain:manage-tokens")
include(":domain:manage-tokens:models")
include(":domain:onramp")
include(":domain:onramp:models")
include(":domain:promo")
include(":domain:promo:models")
include(":domain:nft")
include(":domain:nft:models")
include(":domain:networks")
include(":domain:quotes")
include(":domain:blockaid")
include(":domain:blockaid:models")
include(":domain:notifications")
include(":domain:notifications:models")
include(":domain:express")
include(":domain:express:models")
include(":domain:swap")
include(":domain:swap:models")
include(":domain:wallet-manager")
include(":domain:wallet-manager:models")
// endregion Domain modules

// region Data modules
include(":data:account")
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
include(":data:manage-tokens")
include(":data:networks")
include(":data:nft")
include(":data:onramp")
include(":data:quotes")
include(":data:notifications")
include(":data:blockaid")
include(":data:swap")
include(":data:express")
include(":data:wallet-manager")
// endregion Data modules
include(":features:tangempay:onboarding")