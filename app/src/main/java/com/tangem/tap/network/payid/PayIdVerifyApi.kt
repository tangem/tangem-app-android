package com.tangem.tap.network.payid

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

/**
[REDACTED_AUTHOR]
 */
interface PayIdVerifyApi {
    @GET("{user}")
    suspend fun verifyAddress(
            @Path("user") user: String,
            @Header("Accept") acceptNetworkHeader: String,
            @Header("PayID-Version") payIdVersion: String = "1.0"
    ): VerifyPayIdResponse
}


@JsonClass(generateAdapter = true)
data class VerifyPayIdResponse(
        val addresses: List<PayIdAddress> = mutableListOf(),
        val payId: String? = null,
) {
    fun getAddress(): String? {
        return if (addresses.isEmpty()) null
        else addresses[0].addressDetails.address
    }
}

@JsonClass(generateAdapter = true)
data class PayIdAddress(
        var paymentNetwork: String,
        var environment: String,
        var addressDetailsType: String,
        var addressDetails: PayIdAddressDetails
)

@JsonClass(generateAdapter = true)
data class PayIdAddressDetails(
        var address: String,
        var tag: String? = null
)