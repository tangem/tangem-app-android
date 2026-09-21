package com.tangem.grow.datasource.collectibles

import com.tangem.core.remote.response.ApiResponse
import com.tangem.grow.datasource.collectibles.models.request.GenerateTxRequestBody
import com.tangem.grow.datasource.collectibles.models.request.SubmitTxRequestBody
import com.tangem.grow.datasource.collectibles.models.response.GenerateTxResponse
import com.tangem.grow.datasource.collectibles.models.response.MachineCategoriesResponse
import com.tangem.grow.datasource.collectibles.models.response.MachineNftsResponse
import com.tangem.grow.datasource.collectibles.models.response.MachinesResponse
import com.tangem.grow.datasource.collectibles.models.response.SubmitTxResponse
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

    @RequiresSessionAuth
    @POST("api/v1/collectible/generateTx")
    suspend fun generateTx(
        @Header("user-id") userWalletId: String,
        @Body body: GenerateTxRequestBody,
    ): ApiResponse<GenerateTxResponse>

    @RequiresSessionAuth
    @POST("api/v1/collectible/submitTx")
    suspend fun submitTx(
        @Header("user-id") userWalletId: String,
        @Body body: SubmitTxRequestBody,
    ): ApiResponse<SubmitTxResponse>
}