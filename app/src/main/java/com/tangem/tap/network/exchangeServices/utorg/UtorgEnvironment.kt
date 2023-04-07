package com.tangem.tap.network.exchangeServices.utorg

import android.net.Uri
import com.tangem.datasource.api.common.createRetrofitInstance
import com.tangem.tap.network.exchangeServices.utorg.api.UtorgApi
import com.tangem.tap.network.exchangeServices.utorg.mock.MockUtorgApi
import okhttp3.Interceptor
import okhttp3.Response
import java.util.*

/**
[REDACTED_AUTHOR]
 */
data class UtorgEnvironment(
    val baseUri: Uri,
    val sidValue: String,
    val apiVersion: String,
    val utorgApi: UtorgApi,
    val successUrl: String = "https://success.tangem.com",
) {
    companion object {
        private val PROD_BASE_URL = Uri.parse("https://app.utorg.pro")
        private const val PROD_VERSION = "v1"

        private val STAGE_BASE_URL = Uri.parse("https://app-stage.utorg.pro")
        private const val STAGE_VERSION = "v1"

        fun prod(authProvider: UtorgAuthProvider, apiVersion: String = PROD_VERSION): UtorgEnvironment =
            UtorgEnvironment(
                baseUri = PROD_BASE_URL,
                sidValue = authProvider.sidValue,
                apiVersion = apiVersion,
                utorgApi = createApi(PROD_BASE_URL, authProvider, false),
            )

        fun stage(
            authProvider: UtorgAuthProvider,
            logEnabled: Boolean,
            apiVersion: String = STAGE_VERSION,
        ): UtorgEnvironment = UtorgEnvironment(
            baseUri = STAGE_BASE_URL,
            sidValue = authProvider.sidValue,
            apiVersion = apiVersion,
            utorgApi = createApi(STAGE_BASE_URL, authProvider, logEnabled),
        )

        fun mock(): UtorgEnvironment = UtorgEnvironment(
            baseUri = Uri.EMPTY,
            sidValue = "",
            apiVersion = "",
            utorgApi = MockUtorgApi(),
        )

        private fun createApi(baseUri: Uri, authProvider: UtorgAuthProvider, logEnabled: Boolean): UtorgApi {
            return createRetrofitInstance(
                baseUrl = "$baseUri/",
                interceptors = listOf(UtorgAuthHeaderInterceptor(authProvider)),
                logEnabled = logEnabled,
            ).create(UtorgApi::class.java)
        }
    }
}

data class UtorgAuthProvider(
    val sidValue: String,
    val headerSidKey: String = "X-AUTH-SID",
    val headerNonceKey: String = "X-AUTH-NONCE",
) {
    val clientNonce: String
        get() = UUID.randomUUID().toString()
}

private class UtorgAuthHeaderInterceptor(
    private val authProvider: UtorgAuthProvider,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
            .newBuilder()
            .addHeader(authProvider.headerSidKey, authProvider.sidValue)
            .addHeader(authProvider.headerNonceKey, authProvider.clientNonce)
            .build()

        return chain.proceed(request)
    }
}