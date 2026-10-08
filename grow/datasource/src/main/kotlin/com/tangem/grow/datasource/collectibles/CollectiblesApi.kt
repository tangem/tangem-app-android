package com.tangem.grow.datasource.collectibles

import com.tangem.core.remote.response.ApiResponse
import com.tangem.grow.datasource.collectibles.models.request.CollectibleTxRequestBody
import com.tangem.grow.datasource.collectibles.models.request.GeneratePurchaseTxRequestBody
import com.tangem.grow.datasource.collectibles.models.request.PurchaseTxSentRequestBody
import com.tangem.grow.datasource.collectibles.models.response.CollectibleTxResponse
import com.tangem.grow.datasource.collectibles.models.response.GeneratePurchaseTxResponse
import com.tangem.grow.datasource.collectibles.models.response.MachineCategoriesResponse
import com.tangem.grow.datasource.collectibles.models.response.MachineNftsResponse
import com.tangem.grow.datasource.collectibles.models.response.MachinesResponse
import com.tangem.grow.datasource.collectibles.models.response.model.MachineDTO
import com.tangem.lib.auth.api.RequiresSessionAuth
import retrofit2.http.*

/**
 * Collector Crypt gacha API: machine catalog and collectible purchase transactions.
 *
 * Every endpoint is session-protected, so each method carries [RequiresSessionAuth].
 */
interface CollectiblesApi {

    @RequiresSessionAuth
    @GET("api/v1/machine/categories")
    suspend fun getCategories(): ApiResponse<MachineCategoriesResponse>

    @RequiresSessionAuth
    @GET("api/v1/machine/machines")
    suspend fun getMachines(
        @Query("categoryId") categoryId: String? = null,
        @Query("search") search: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("cursor") cursor: String? = null,
    ): ApiResponse<MachinesResponse>

    /**
     * Machine by its [externalId] (e.g. `pokemon_50`), not by the internal `id`.
     */
    @RequiresSessionAuth
    @GET("api/v1/machine/{externalId}")
    suspend fun getMachine(@Path("externalId") externalId: String): ApiResponse<MachineDTO>

    /**
     * Collectibles currently in the prize pool of the machine [externalId], paged by [page] (1-based).
     */
    @RequiresSessionAuth
    @GET("api/v1/machine/{externalId}/nfts")
    suspend fun getMachineNfts(
        @Header("user-id") userWalletId: String,
        @Path("externalId") externalId: String,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
    ): ApiResponse<MachineNftsResponse>

    /** Issues the gacha's memo and price for one spin; the app builds, signs and sends the transaction itself. */
    @RequiresSessionAuth
    @POST("api/v1/collectible/generate-purchase-tx")
    suspend fun generatePurchaseTx(
        @Header("user-id") userWalletId: String,
        @Body body: GeneratePurchaseTxRequestBody,
    ): ApiResponse<GeneratePurchaseTxResponse>

    @RequiresSessionAuth
    @POST("api/v1/collectible/purchase-tx-sent")
    suspend fun purchaseTxSent(
        @Header("user-id") userWalletId: String,
        @Body body: PurchaseTxSentRequestBody,
    ): ApiResponse<CollectibleTxResponse>

    @RequiresSessionAuth
    @GET("api/v1/collectible/status")
    suspend fun getTxStatus(
        @Header("user-id") userWalletId: String,
        @Query("txId") txId: String,
        @Query("includeNftWon") includeNftWon: Boolean,
    ): ApiResponse<CollectibleTxResponse>

    /** Only meaningful once the purchase is confirmed; before that the gacha answers "No spin transaction found". */
    @RequiresSessionAuth
    @POST("api/v1/collectible/open-pack")
    suspend fun openPack(
        @Header("user-id") userWalletId: String,
        @Body body: CollectibleTxRequestBody,
        @Query("includeNftWon") includeNftWon: Boolean,
    ): ApiResponse<CollectibleTxResponse>
}