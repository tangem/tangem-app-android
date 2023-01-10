package com.tangem.datasource.api.paymentology

import com.tangem.datasource.api.paymentology.models.request.CheckRegistrationRequests
import com.tangem.datasource.api.paymentology.models.request.RegisterKYCRequest
import com.tangem.datasource.api.paymentology.models.request.RegisterWalletRequest
import com.tangem.datasource.api.paymentology.models.response.AttestationResponse
import com.tangem.datasource.api.paymentology.models.response.RegisterWalletResponse
import com.tangem.datasource.api.paymentology.models.response.RegistrationResponse
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * Interface of Paymentology Api.
 *
 * IMPORTANT: Cannot replace [Headers] annotations with OkHttpClient.addHeader(), because OkHttp adds encoding to
 * content type value. But Paymentology API expects a value without the charset.
 *
 * @author Anton Zhilenkov on 06.10.2022.
 */
interface PaymentologyApi {

    @Headers("Content-Type: application/json")
    @POST("card/verify")
    suspend fun checkRegistration(@Body request: CheckRegistrationRequests): RegistrationResponse

    @Headers("Content-Type: application/json")
    @POST("card/get_challenge")
    suspend fun requestAttestationChallenge(@Body request: CheckRegistrationRequests.Item): AttestationResponse

    @Headers("Content-Type: application/json")
    @POST("card/set_pin")
    suspend fun registerWallet(@Body request: RegisterWalletRequest): RegisterWalletResponse

    @Headers("Content-Type: application/json")
    @POST("card/kyc")
    suspend fun registerKYC(@Body request: RegisterKYCRequest): RegisterWalletResponse
}
