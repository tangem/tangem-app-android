package com.tangem.datasource.config.models

import org.spongycastle.util.encoders.Base64.toBase64String

/**
[REDACTED_AUTHOR]
 */
data class SaltPayConfig(
    val zendesk: ZendeskConfig,
    val kycProvider: KYCProvider,
    val credentials: Credentials,
) {
    companion object {
        fun stub(): SaltPayConfig {
            return SaltPayConfig(
                zendesk = ZendeskConfig("", "", "", "", ""),
                kycProvider = KYCProvider("", "", "", ""),
                credentials = Credentials("", ""),
            )
        }
    }
}

data class KYCProvider(
    val baseUrl: String,
    val externalIdParameterKey: String,
    val sidParameterKey: String,
    val sidValue: String,
)

data class Credentials(
    val user: String,
    val password: String,
) {
    val token: String by lazy { "Basic ${toBase64String("$user:$password".toByteArray())}" }
}