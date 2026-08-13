package com.tangem.datasource.api.jointaccount

import com.tangem.core.remote.response.ApiResponse
import com.tangem.datasource.api.jointaccount.models.CreateJointAccountRequest
import com.tangem.datasource.api.jointaccount.models.GetJointAccountsResponse
import com.tangem.datasource.api.jointaccount.models.JointAccountDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Joint accounts API, stage 1: creation and retrieval.
 *
 * Contract: `.claude/docs/joint-accounts/joint-accounts-openapi_v1.1.yaml`.
 */
interface JointAccountApi {

    /**
     * Creates the account in `pending` with the caller as its creator and first participant, and issues one invite
     * per free slot.
     *
     * Errors: 400 — field validation, `creator.walletId` not equal to the path, or a signature that does not recover
     * to `creator.address`; 409 — `creator.address` is already registered in a joint account. On 409 do NOT retry
     * under the next derivation index — resolve with [getJointAccounts], which will show that account.
     */
    @POST("v1/wallets/{walletId}/joint-accounts")
    suspend fun createJointAccount(
        @Path("walletId") walletId: String,
        @Body body: CreateJointAccountRequest,
    ): ApiResponse<JointAccountDto>

    /**
     * Returns every joint account the wallet participates in, archived ones included.
     *
     * Not cacheable — `If-None-Match` is not supported, since participants join in other wallets and move no version
     * of this one. Refresh on push and on screen opening.
     */
    @GET("v1/wallets/{walletId}/joint-accounts")
    suspend fun getJointAccounts(@Path("walletId") walletId: String): ApiResponse<GetJointAccountsResponse>
}