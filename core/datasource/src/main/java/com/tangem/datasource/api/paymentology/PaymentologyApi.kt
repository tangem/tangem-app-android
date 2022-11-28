package com.tangem.datasource.api.paymentology

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * Created by Anton Zhilenkov on 06.10.2022.
 */
interface PaymentologyApi {

    @Headers("Content-Type: application/json")
    @POST("card/verify")
    suspend fun checkRegistration(
        @Body request: CheckRegistrationRequests,
    ): RegistrationResponse

    @Headers("Content-Type: application/json")
    @POST("card/get_challenge")
    suspend fun requestAttestationChallenge(
        @Body request: CheckRegistrationRequests.Item,
    ): AttestationResponse

    @Headers("Content-Type: application/json")
    @POST("card/set_pin")
    suspend fun registerWallet(
        @Body request: RegisterWalletRequest,
    ): RegisterWalletResponse

    @Headers("Content-Type: application/json")
    @POST("card/kyc")
    suspend fun registerKYC(
        @Body request: RegisterKYCRequest,
    ): RegisterWalletResponse

    companion object {
        val baseUrl: String = "https://paymentologygate.oa.r.appspot.com/"
    }
}
