package com.tangem.tap.features.onboarding.products.wallet.saltPay

import com.tangem.tap.common.zendesk.ZendeskConfig
import org.spongycastle.util.encoders.Base64

/**
 * Created by Anton Zhilenkov on 13.10.2022.
 */
data class SaltPayConfig(
    val zendesk: ZendeskConfig,
    val kycProvider: KYCProvider,
    val credentials: AuthCredentials,
) {
    companion object {
        fun stub(): SaltPayConfig {
            return SaltPayConfig(
                zendesk = ZendeskConfig("", "", "", "", ""),
                kycProvider = KYCProvider("", "", "", ""),
                credentials = AuthCredentials("", ""),
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

data class AuthCredentials(
    val user: String,
    val password: String,
) {
    val authToken: String by lazy {
        val data = "$user:$password".toByteArray()
        val base64 = Base64.toBase64String(data)
        "Basic $base64".trim()
    }
}
