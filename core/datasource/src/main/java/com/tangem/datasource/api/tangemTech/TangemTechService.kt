package com.tangem.datasource.api.tangemTech

import com.tangem.datasource.api.common.MoshiConverter
import com.tangem.datasource.utils.RequestHeader
import com.tangem.datasource.utils.RequestHeader.AuthenticationHeader
import com.tangem.datasource.utils.RequestHeader.CacheControlHeader
import com.tangem.datasource.utils.addHeaders
import com.tangem.datasource.utils.allowLogging
import okhttp3.OkHttpClient
import retrofit2.Retrofit

/**
 * @author Anton Zhilenkov on 02/04/2022
 */
// TODO("Remove after removing Redux")
@Deprecated("Use TangemTechApi")
object TangemTechService {

    var api: TangemTechApi = createApi()
        private set

    private const val TANGEM_TECH_BASE_URL = "https://api.tangem-tech.com/v1/"

    fun addAuthenticationHeader(header: AuthenticationHeader) {
        api = createApi(header)
    }

    private fun createApi(header: RequestHeader? = null): TangemTechApi {
        val headers = mutableListOf<RequestHeader>(CacheControlHeader).apply { header?.let(::add) }
        return Retrofit.Builder()
            .addConverterFactory(MoshiConverter.networkMoshiConverter)
            .baseUrl(TANGEM_TECH_BASE_URL)
            .client(
                OkHttpClient.Builder()
                    .addHeaders(*headers.toTypedArray())
                    .allowLogging()
                    .build(),
            )
            .build()
            .create(TangemTechApi::class.java)
    }
}
