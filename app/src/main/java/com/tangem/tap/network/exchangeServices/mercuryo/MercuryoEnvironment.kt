package com.tangem.tap.network.exchangeServices.mercuryo

import com.tangem.datasource.api.common.createRetrofitInstance

/**
[REDACTED_AUTHOR]
 */
data class MercuryoEnvironment(
    val baseUrl: String,
    val apiVersion: String,
    val widgetId: String,
    val secret: String,
    val mercuryoApi: MercuryoApi,
) {
    companion object {
        private const val BASE_URL = "https://api.mercuryo.io/"
        private const val API_VERSION = "v1.6"

        fun prod(widgetId: String, secret: String, apiVersion: String = API_VERSION): MercuryoEnvironment {
            return MercuryoEnvironment(
                baseUrl = BASE_URL,
                apiVersion = apiVersion,
                widgetId = widgetId,
                secret = secret,
                mercuryoApi = createRetrofitInstance(
                    baseUrl = BASE_URL,
                    logEnabled = false,
                ).create(MercuryoApi::class.java),
            )
        }
    }
}