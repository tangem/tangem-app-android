package com.tangem.datasource.config.models

/**
 * Created by Anton Zhilenkov on 13.10.2022.
 */
data class SaltPayConfig(
    val sprinklr: SprinklrConfig,
    val kycProvider: KYCProvider,
    val credentials: Credentials,
    val blockscoutCredentials: Credentials,
) {
    companion object {
        fun stub(): SaltPayConfig {
            return SaltPayConfig(
                sprinklr = SprinklrConfig("", ""),
                kycProvider = KYCProvider("", "", "", ""),
                credentials = Credentials("", ""),
                blockscoutCredentials = Credentials("", ""),
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
    val basicAuthToken: String by lazy { okhttp3.Credentials.basic(user, password) }
}
