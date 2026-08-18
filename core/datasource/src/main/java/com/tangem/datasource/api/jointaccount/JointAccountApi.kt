package com.tangem.datasource.api.jointaccount

import com.tangem.core.remote.response.ApiResponse
import com.tangem.datasource.api.jointaccount.models.ActivateJointAccountRequest
import com.tangem.datasource.api.jointaccount.models.CreateJointAccountRequest
import com.tangem.datasource.api.jointaccount.models.GetJointAccountsResponse
import com.tangem.datasource.api.jointaccount.models.JointAccountDto
import com.tangem.datasource.api.jointaccount.models.JointAccountInvitePreviewDto
import com.tangem.datasource.api.jointaccount.models.JoinJointAccountRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Joint accounts API: creation, retrieval, joining, activation.
 *
 * Contract: OpenAPI spec v1.2.
 */
interface JointAccountApi {

    /**
     * Creates the account in `pending` with the caller as its creator and first participant, and issues one invite
     * per free slot.
     *
     * Errors: 400 — field validation, `creator.walletId` not equal to the path, a signature that does not recover
     * to `creator.address`, or the wallet already participates in 20 active joint accounts; 409 — `creator.address`
     * is already registered in a joint account. On 409 do NOT retry under the next derivation index — resolve with
     * [getJointAccounts], which will show that account.
     */
    @POST("v1/wallets/{walletId}/joint-accounts")
    suspend fun createJointAccount(
        @Path("walletId") walletId: String,
        @Body body: CreateJointAccountRequest,
    ): ApiResponse<JointAccountDto>

    /**
     * Returns every joint account the wallet participates in, archived ones excluded — they come from
     * `/accounts/archived`, and this endpoint is refetched after unarchiving. The free derivation index does not
     * come from here: it is `totalJointAccounts` in `GET /accounts`.
     *
     * Not cacheable — `If-None-Match` is not supported, since participants join in other wallets and move no version
     * of this one. Refresh on push and on screen opening.
     */
    @GET("v1/wallets/{walletId}/joint-accounts")
    suspend fun getJointAccounts(@Path("walletId") walletId: String): ApiResponse<GetJointAccountsResponse>

    /**
     * Shows the invitee what he is joining: the configuration and who is inviting. The path is wallet-scoped even
     * though the invitee has not yet decided which wallet to join with — that is what lets the preview report the
     * wallet already holds a slot before the card is tapped rather than after.
     *
     * Errors: 404 — no such invite; 409 — the invite is already redeemed, or this wallet already holds a slot.
     */
    @GET("v1/wallets/{walletId}/joint-accounts/invites/{inviteId}")
    suspend fun getJointAccountInvite(
        @Path("walletId") walletId: String,
        @Path("inviteId") inviteId: String,
    ): ApiResponse<JointAccountInvitePreviewDto>

    /**
     * Takes the slot behind the invite and creates the joiner's own row in `accounts`. Taking the last slot computes
     * the Safe address and moves the account to `confirming`.
     *
     * Errors: 400 — field validation, `member.walletId` not equal to the path, a signature that does not recover to
     * `member.address`, or the wallet already participates in 20 active joint accounts; 404 — no such invite or
     * wallet. Every 409 is terminal for that invite link, except `member.address` already registered, which is
     * resolved as in creation. A joiner who lost the response finds the account in [getJointAccounts]; retrying
     * is 409.
     */
    @POST("v1/wallets/{walletId}/joint-accounts/join")
    suspend fun joinJointAccount(
        @Path("walletId") walletId: String,
        @Body body: JoinJointAccountRequest,
    ): ApiResponse<JointAccountDto>

    /**
     * Confirms the composition and activates the account. Idempotent: activating an already `active` account is a
     * success, so a lost response is retried automatically rather than shown as an error.
     *
     * Errors: 400 — `payload.walletId` not equal to the path, a malformed signature, or one that does not recover
     * to this wallet's own participant address; 403 — this wallet's slot is not the creator's; 404 — this wallet
     * has no such joint account; 409 — the account is still `pending`, or `config` differs from what the account
     * holds.
     */
    @POST("v1/wallets/{walletId}/joint-accounts/activate")
    suspend fun activateJointAccount(
        @Path("walletId") walletId: String,
        @Body body: ActivateJointAccountRequest,
    ): ApiResponse<JointAccountDto>
}