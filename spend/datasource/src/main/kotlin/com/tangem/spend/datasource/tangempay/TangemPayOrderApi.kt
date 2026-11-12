package com.tangem.spend.datasource.tangempay

import com.tangem.core.remote.response.ApiResponse
import com.tangem.spend.datasource.tangempay.models.request.OrderRequest
import com.tangem.spend.datasource.tangempay.models.response.TangemPayOrderResponse
import com.tangem.spend.datasource.tangempay.models.response.TangemPayOrdersResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TangemPayOrderApi {

    @GET("v1/order/{order_id}")
    suspend fun getOrder(
        @Header("Authorization") authHeader: String,
        @Path("order_id") orderId: String,
    ): ApiResponse<TangemPayOrderResponse>

    @GET("v1/order")
    suspend fun findOrders(
        @Header("Authorization") authHeader: String,
        @Query("order_types") orderTypes: List<String>,
        @Query("order_statuses") orderStatuses: List<String>,
    ): ApiResponse<TangemPayOrdersResponse>

    @POST("v1/order")
    suspend fun createOrder(
        @Header("Authorization") authHeader: String,
        @Header("X-Idempotency-Key") idempotencyKey: String,
        @Body body: OrderRequest,
    ): ApiResponse<TangemPayOrderResponse>
}