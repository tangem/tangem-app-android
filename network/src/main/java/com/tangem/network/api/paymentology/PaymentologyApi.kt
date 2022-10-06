package com.tangem.network.api.paymentology

import retrofit2.http.Body
import retrofit2.http.POST

/**
[REDACTED_AUTHOR]
 */
interface PaymentologyApi {

    @POST("card/verify")
    fun checkRegistration(
        @Body request: CheckRegistrationRequests,
    ): RegistrationResponse.Item

    @POST("card/get_challenge")
    fun requestAttestationChallenge(
        @Body request: CheckRegistrationRequests.Item,
    ): AttestationResponse

    @POST("card/set_pin")
    fun registerWallet(
        @Body request: RegisterWalletRequest,
    ): RegisterWalletResponse

    companion object {
        val baseUrl: String = "https://paymentologygate.oa.r.appspot.com/"
    }
}