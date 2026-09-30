package com.tangem.plugin.settings

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.util.Base64
import java.util.Properties

abstract class MeaWalletSdkAccessDenial : ValueSource<String, MeaWalletSdkAccessDenial.Parameters> {

    interface Parameters : ValueSourceParameters {
        val metadataUrl: Property<String>
        val localProperties: RegularFileProperty
    }

    override fun obtain(): String? {
        val gprProperties = Properties()
        parameters.localProperties.get().asFile.takeIf { it.isFile }?.inputStream()?.use { gprProperties.load(it) }
        val user = gprProperties.getProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR").orEmpty()
        val token = gprProperties.getProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
            ?: return "there is no gpr.key or GITHUB_TOKEN for GitHub Packages"
        val credentials = Base64.getEncoder().encodeToString("$user:$token".toByteArray())
        val answer = try {
            val connection = URI(parameters.metadataUrl.get()).toURL().openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = TIMEOUT_MILLIS
            connection.readTimeout = TIMEOUT_MILLIS
            connection.setRequestProperty("Authorization", "Basic $credentials")
            "HTTP ${connection.responseCode}"
        } catch (e: IOException) {
            "${e.javaClass.simpleName}: ${e.message}"
        }
        return when (answer) {
            "HTTP 200" -> null
            in DENIED_ANSWERS -> "GitHub Packages refused the token ($answer)"
            else -> {
                println(
                    "Could not check the MeaWallet SDK in GitHub Packages ($answer); building with it, " +
                        "set meawallet.sdk=noop if it cannot be resolved",
                )
                null
            }
        }
    }

    private companion object {
        val DENIED_ANSWERS = setOf("HTTP 401", "HTTP 403", "HTTP 404")
        const val TIMEOUT_MILLIS = 3_000
    }
}