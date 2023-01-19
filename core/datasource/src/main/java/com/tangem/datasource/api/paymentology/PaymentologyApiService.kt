package com.tangem.datasource.api.paymentology

import com.tangem.datasource.api.common.MoshiConverter
import com.tangem.datasource.utils.allowLogging
import okhttp3.OkHttpClient
import retrofit2.Retrofit

/**
 * @author Anton Zhilenkov on 06.10.2022.
 */
// TODO("Remove after removing Redux")
@Deprecated("Use PaymentologyApi")
object PaymentologyApiService {
    val api = createApi()

    private const val PAYMENTOLOGY_BASE_URL: String = "https://paymentologygate.oa.r.appspot.com/"

    private fun createApi(): PaymentologyApi {
        return Retrofit.Builder()
            .addConverterFactory(MoshiConverter.networkMoshiConverter)
            .baseUrl(PAYMENTOLOGY_BASE_URL)
            .client(
                OkHttpClient.Builder()
                    .allowLogging()
                    .build(),
            )
            .build()
            .create(PaymentologyApi::class.java)
    }
}
