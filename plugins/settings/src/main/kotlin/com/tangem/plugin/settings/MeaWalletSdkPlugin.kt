package com.tangem.plugin.settings

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import java.io.StringReader
import java.net.URI
import java.util.Properties

private const val REGISTRY_URL = "https://maven.pkg.github.com/tangem-developments/tangem-app-android"
private const val METADATA_URL = "$REGISTRY_URL/com/meawallet/mpp-prod/maven-metadata.xml"
private const val CONFIG_PATH = "app/src/main/assets/tangem-app-config/meawallet/mea_config"
private const val LOCAL_PROPERTIES = "local.properties"
private const val SDK_PROPERTY = "meawallet.sdk"
private const val REQUIRE_SDK_VARIABLE = "REQUIRE_MEAWALLET_SDK"
private const val IMPL_PROJECT = ":spend:push-provisioning:impl"
private const val PACKAGES_TOKEN_VARIABLE = "GITHUB_PACKAGES_TOKEN"

class MeaWalletSdkPlugin : Plugin<Settings> {

    override fun apply(settings: Settings) {
        val properties = Properties()
        settings.providers.fileContents(settings.layout.rootDirectory.file(LOCAL_PROPERTIES)).asText.orNull
            ?.let { properties.load(StringReader(it)) }

        addRegistry(settings, properties)

        val noopReason = findNoopReason(settings, properties)
        if (noopReason == null) {
            settings.include(IMPL_PROJECT)
            return
        }

        println("Google Wallet push provisioning is built as a no-op: $noopReason")
        if (settings.providers.environmentVariable(REQUIRE_SDK_VARIABLE).orNull == "true") {
            error("$REQUIRE_SDK_VARIABLE is set, but the MeaWallet SDK is not available to this build: $noopReason")
        }
    }

    private fun addRegistry(settings: Settings, properties: Properties) {
        val repositories = settings.dependencyResolutionManagement.repositories
        repositories.exclusiveContent {
            forRepository {
                repositories.maven {
                    url = URI(REGISTRY_URL)
                    credentials {
                        username = properties.getProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
                        password = properties.gitHubPackagesToken()
                    }
                }
            }
            filter {
                includeGroupAndSubgroups("com.meawallet")
                includeModule("com.google.android.gms", "play-services-tapandpay")
            }
        }
    }

    private fun findNoopReason(settings: Settings, properties: Properties): String? {
        val hasConfig = settings.rootDir.resolve(CONFIG_PATH).isFile
        val override = (settings.providers.gradleProperty(SDK_PROPERTY).orNull ?: properties.getProperty(SDK_PROPERTY))
            ?.trim()
            ?.let { if (it == "no-op") "noop" else it }
        require(override == null || override == "impl" || override == "noop") {
            "$SDK_PROPERTY must be impl or noop, got '$override'"
        }
        require(override != "impl" || hasConfig) { "$SDK_PROPERTY=impl needs meawallet/mea_config in tangem-app-config" }

        return when {
            !hasConfig -> "tangem-app-config has no meawallet/mea_config"
            override == "noop" -> "$SDK_PROPERTY=noop"
            override == null && !settings.gradle.startParameter.isOffline -> findAccessDenial(settings)
            else -> null
        }
    }

    private fun findAccessDenial(settings: Settings): String? {
        val denial = settings.providers.of(MeaWalletSdkAccessDenial::class.java) {
            parameters.metadataUrl.set(METADATA_URL)
            parameters.localProperties.set(settings.layout.rootDirectory.file(LOCAL_PROPERTIES))
        }.orNull ?: return null

        return "$denial; the MeaWallet SDK needs a token with read:packages and SSO for tangem-developments"
    }
}

internal fun Properties.gitHubPackagesToken(): String? =
    getProperty("gpr.key") ?: System.getenv(PACKAGES_TOKEN_VARIABLE) ?: System.getenv("GITHUB_TOKEN")