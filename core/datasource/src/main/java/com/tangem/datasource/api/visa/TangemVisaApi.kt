package com.tangem.datasource.api.visa

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.utils.ReadTimeout
import com.tangem.datasource.api.visa.models.request.ActivationByCardWalletRequest
import com.tangem.datasource.api.visa.models.request.ActivationByCustomerWalletRequest
import com.tangem.datasource.api.visa.models.request.SetPinCodeRequest
import com.tangem.datasource.api.visa.models.response.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface TangemVisaApi {

    @GET("product_instance/activation_status")
    suspend fun getRemoteActivationStatus(
        @Header("Authorization") authHeader: String,
        @Query("customer_id") customerId: String,
        @Query("product_instance_id") productInstanceId: String,
        @Query("card_id") cardId: String,
        @Query("card_public_key") cardPublicKey: String,
    ): ApiResponse<CardActivationRemoteStateResponse>

    @ReadTimeout(duration = 20, TimeUnit.MINUTES)
    @GET("product_instance/activation_status")
    suspend fun getRemoteActivationStatusLongPoll(
        @Header("Authorization") authHeader: String,
        @Query("customer_id") customerId: String,
        @Query("product_instance_id") productInstanceId: String,
        @Query("card_id") cardId: String,
        @Query("card_public_key") cardPublicKey: String,
    ): ApiResponse<CardActivationRemoteStateResponse>

    @GET("product_instance/card_wallet_acceptance")
    suspend fun getCardWalletAcceptance(
        @Header("Authorization") authHeader: String,
        @Query("customer_id") customerId: String,
        @Query("product_instance_id") productInstanceId: String,
        @Query("activation_order") activationOrderId: String,
        @Query("customer_wallet_address") customerWalletAddress: String,
    ): ApiResponse<CardWalletDataToSignResponse>

    @GET("product_instance/customer_wallet_acceptance")
    suspend fun getCustomerWalletAcceptance(
        @Header("Authorization") authHeader: String,
        @Query("customer_id") customerId: String,
        @Query("product_instance_id") productInstanceId: String,
        @Query("activation_order") activationOrderId: String,
        @Query("card_wallet_address") cardWalletAddress: String,
    ): ApiResponse<CustomerWalletDataToSignResponse>

    @POST("product_instance/activation_by_card_wallet")
    suspend fun activateByCardWallet(
        @Header("Authorization") authHeader: String,
        @Body body: ActivationByCardWalletRequest,
    ): ApiResponse<Unit>

    @POST("product_instance/activation_by_customer_wallet")
    suspend fun activateByCustomerWallet(
        @Header("Authorization") authHeader: String,
        @Body body: ActivationByCustomerWalletRequest,
    ): ApiResponse<Unit>

    @POST("product_instance/issuer_activation")
    suspend fun setPinCode(
        @Header("Authorization") authHeader: String,
        @Body body: SetPinCodeRequest,
    ): ApiResponse<Unit>

    @GET("customer/info")
    suspend fun getCustomerInfo(
        @Header("Authorization") authHeader: String,
        @Query("card_id") cardId: String,
    ): ApiResponse<VisaCustomerInfo>

    @GET("product_instance/transactions")
    suspend fun getTxHistory(
        @Header("Authorization") authHeader: String,
        @Query("customer_id") customerId: String,
        @Query("product_instance_id") productInstanceId: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
    ): ApiResponse<VisaTxHistoryResponse>
}