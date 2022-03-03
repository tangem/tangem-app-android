package com.tangem.tap.network.exchangeServices.onramper

import com.squareup.moshi.JsonClass
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface OnramperApi {
    @GET("gateways")
    suspend fun gateways(): GatewaysResponse

    @GET("rate/{fromCurrency}/{toCurrency}/{paymentMethod}/{amount}")
    suspend fun rate(
        @Path("fromCurrency") fromCurrency: String,
        @Path("toCurrency") toCurrency: String,
        @Path("paymentMethod") paymentMethod: String,
        @Path("amount") amount: Int,
    ): RateResponse

    companion object {
        val BASE_URL = "https://onramper.tech/"
    }
}

class AddKeyToHeaderInterceptor(
    private val key: String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder().addHeader("Authorization", "Basic $key").build()
        return chain.proceed(request)
    }
}

@JsonClass(generateAdapter = true)
data class GatewaysResponse(
    val gateways: List<OnramperGateway>
)

@JsonClass(generateAdapter = true)
data class OnramperGateway(
    val identifier: String,
    val paymentMethods: List<String>,
    val fiatCurrencies: List<OnramperCurrency>,
    val cryptoCurrencies: List<OnramperCurrency>
)

@JsonClass(generateAdapter = true)
data class OnramperCurrency(
    val id: String,
    val code: String,
    val precision: Int
)

@JsonClass(generateAdapter = true)
data class RateResponse(
    val identifier: String,
    val duration: OnramperDuration,
    val available: Boolean,
    val error: OnramperError? = null,
    val rate: Double? = null,
    val fees: Double? = null,
    val requiredKYC: List<String>? = null,
    val receivedCrypto: Double? = null,
    val nextStep: OnramperNextStep? = null,
)

@JsonClass(generateAdapter = true)
data class OnramperNextStep(
    val type: String,
    val url: String,
    val message: String,
    val extraData: List<OnramperExtraData>
)

@JsonClass(generateAdapter = true)
data class OnramperExtraData(
    val type: String,
    val name: String,
    val humanName: String,
)

@JsonClass(generateAdapter = true)
data class OnramperDuration(
    val seconds: Long,
    val message: String
)

@JsonClass(generateAdapter = true)
data class OnramperError(
    val type: String,
    val message: String,
    val limit: Double
)