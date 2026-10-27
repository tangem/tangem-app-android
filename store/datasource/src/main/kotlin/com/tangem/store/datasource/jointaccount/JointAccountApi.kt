package com.tangem.store.datasource.jointaccount

import com.tangem.core.remote.response.ApiResponse
import com.tangem.store.datasource.jointaccount.models.ConfirmJointAccountRequest
import com.tangem.store.datasource.jointaccount.models.CreateJointAccountRequest
import com.tangem.store.datasource.jointaccount.models.CreateJointOperationRequest
import com.tangem.store.datasource.jointaccount.models.GetJointAccountsResponse
import com.tangem.store.datasource.jointaccount.models.IssueJointAccountInviteRequest
import com.tangem.store.datasource.jointaccount.models.JointAccountDto
import com.tangem.store.datasource.jointaccount.models.JointAccountInvitePreviewDto
import com.tangem.store.datasource.jointaccount.models.JointAccountIssuedInviteDto
import com.tangem.store.datasource.jointaccount.models.JointOperationDto
import com.tangem.store.datasource.jointaccount.models.JointOperationHistoryDeltaResponse
import com.tangem.store.datasource.jointaccount.models.JointOperationHistoryItemDto
import com.tangem.store.datasource.jointaccount.models.JointOperationHistoryResponse
import com.tangem.store.datasource.jointaccount.models.JoinJointAccountRequest
import com.tangem.store.datasource.jointaccount.models.SignJointOperationRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Joint accounts API: creation, retrieval, joining, confirmation.
 *
 * Contract: OpenAPI spec v1.6.
 *
 * The paths carry the `api/` prefix, unlike the neighbouring `/v1/wallets/{walletId}/accounts` of
 * `TangemTechApi`. That is not a typo and not a copy-paste: v1.2 moved the joint endpoints under `/api/v1`, and
 * the two prefixes are different routes at the edge — the wrong one answers 403 with a Cloudflare page instead of
 * a 404, so a mistake here looks like a blocked feature rather than a missing path.
 */
interface JointAccountApi {

    /**
     * Creates the account in `pending` with the caller as its creator and first participant. The invites are not
     * issued here: the creator issues one per free slot with [issueJointAccountInvite] (v1.6).
     *
     * Errors: 400 — field validation, `creator.walletId` not equal to the path, a signature that does not recover
     * to `creator.address`, or the wallet already participates in 20 active joint accounts; 409 — `creator.address`
     * is already registered in a joint account. On 409 do NOT retry under the next derivation index — resolve with
     * [getJointAccounts], which will show that account.
     */
    @POST("api/v1/wallets/{walletId}/joint-accounts")
    suspend fun createJointAccount(
        @Path("walletId") walletId: String,
        @Body body: CreateJointAccountRequest,
    ): ApiResponse<JointAccountDto>

    /**
     * Issues the invite of one free slot of the creator's `pending` account. The code comes back only here and lives
     * 24 hours; a repeated call on the same slot replaces it, and the old link answers 404 from then on.
     *
     * Errors: 400 — `slot` outside `1..membersCount - 1`; 403 — a member, not the creator; 404 — no such account for
     * the wallet; 409 — the account left `pending`, or the slot is taken.
     */
    @POST("api/v1/wallets/{walletId}/joint-accounts/invites")
    suspend fun issueJointAccountInvite(
        @Path("walletId") walletId: String,
        @Body body: IssueJointAccountInviteRequest,
    ): ApiResponse<JointAccountIssuedInviteDto>

    /**
     * Returns every joint account the wallet participates in, archived ones excluded — they come from
     * `/accounts/archived`, and this endpoint is refetched after unarchiving. The free derivation index does not
     * come from here: it is `totalJointAccounts` in `GET /accounts`.
     *
     * The response carries an ETag covering everything the list shows — a member, a status, an operation, a vote —
     * so [eTag] asks the backend to answer `304` while nothing of it moved. Refresh on push and on screen opening.
     */
    @GET("api/v1/wallets/{walletId}/joint-accounts")
    suspend fun getJointAccounts(
        @Path("walletId") walletId: String,
        @Header("If-None-Match") eTag: String? = null,
    ): ApiResponse<GetJointAccountsResponse>

    /**
     * Shows the invitee what he is joining: the configuration and who is inviting. The path is wallet-scoped even
     * though the invitee has not yet decided which wallet to join with — that is what lets the preview report the
     * wallet already holds a slot before the card is tapped rather than after.
     *
     * Errors: 404 — no such invite, a code replaced by a reissue included; 409 — the invite expired or is already
     * redeemed, the account no longer accepts participants, or this wallet already holds a slot.
     */
    @GET("api/v1/wallets/{walletId}/joint-accounts/invites/{inviteId}")
    suspend fun getJointAccountInvite(
        @Path("walletId") walletId: String,
        @Path("inviteId") inviteId: String,
    ): ApiResponse<JointAccountInvitePreviewDto>

    /**
     * Takes the slot behind the invite and creates the joiner's own row in `accounts`. Taking the last slot computes
     * the Safe address and moves the account to `confirming`.
     *
     * Errors: 400 — field validation, `member.walletId` not equal to the path, a signature that does not recover to
     * `member.address`, or the wallet already participates in 20 active joint accounts; 404 — no such invite (a
     * code replaced by a reissue included) or wallet. Every 409 is terminal for that invite link — an expired
     * invite among them — except `member.address` already registered, which is resolved as in creation. A joiner who lost the response finds the account in [getJointAccounts]; retrying
     * is 409.
     */
    @POST("api/v1/wallets/{walletId}/joint-accounts/join")
    suspend fun joinJointAccount(
        @Path("walletId") walletId: String,
        @Body body: JoinJointAccountRequest,
    ): ApiResponse<JointAccountDto>

    /**
     * Records this wallet's confirmation of the composition; the account turns `active` with the last member's
     * signature. Idempotent: a repeated confirmation is the same success, so a lost response is retried
     * automatically.
     *
     * Errors: 400 — a malformed signature, one that does not recover to this wallet's slot, or `safeAddress`
     * not in lower case; 403 — this wallet is not a member; 404 — no account holds this Safe address, a
     * `pending` account included; 409 — the account is cancelled, or `payload` differs from what the account
     * holds.
     */
    @POST("api/v1/wallets/{walletId}/joint-accounts/confirm")
    suspend fun confirmJointAccount(
        @Path("walletId") walletId: String,
        @Body body: ConfirmJointAccountRequest,
    ): ApiResponse<JointAccountDto>

    @POST("api/v1/wallets/{walletId}/joint-accounts/operations")
    suspend fun createJointOperation(
        @Path("walletId") walletId: String,
        @Body body: CreateJointOperationRequest,
    ): ApiResponse<JointOperationDto>

    @POST("api/v1/wallets/{walletId}/joint-accounts/operations/{operationId}/sign")
    suspend fun signJointOperation(
        @Path("walletId") walletId: String,
        @Path("operationId") operationId: String,
        @Body body: SignJointOperationRequest,
    ): ApiResponse<JointOperationDto>

    @POST("api/v1/wallets/{walletId}/joint-accounts/operations/{operationId}/reject")
    suspend fun rejectJointOperation(
        @Path("walletId") walletId: String,
        @Path("operationId") operationId: String,
    ): ApiResponse<Unit>

    /**
     * Operations of every joint account the wallet holds a slot in, archived ones included, in every status.
     *
     * @param afterCursor `endCursor` of the previous page; absent on the first page
     */
    @GET("api/v1/wallets/{walletId}/joint-accounts/operations/history")
    suspend fun getJointOperationHistory(
        @Path("walletId") walletId: String,
        @Query("afterCursor") afterCursor: String?,
        @Query("limit") limit: Int,
    ): ApiResponse<JointOperationHistoryResponse>

    /**
     * Operations that changed after the cursor. The client upserts by `operationId`.
     *
     * @param beforeCursor `startDeltaCursor` of the first history page, then `startCursor` of the previous delta page
     */
    @GET("api/v1/wallets/{walletId}/joint-accounts/operations/history/delta")
    suspend fun getJointOperationHistoryDelta(
        @Path("walletId") walletId: String,
        @Query("beforeCursor") beforeCursor: String?,
        @Query("limit") limit: Int,
    ): ApiResponse<JointOperationHistoryDeltaResponse>

    /**
     * One operation in any status. [eTag] asks the backend to answer `304` while the operation has not changed.
     *
     * Errors: 403 — the wallet holds no slot in the operation's joint account; 404 — no such operation.
     */
    @GET("api/v1/wallets/{walletId}/joint-accounts/operations/{operationId}")
    suspend fun getJointOperation(
        @Path("walletId") walletId: String,
        @Path("operationId") operationId: String,
        @Header("If-None-Match") eTag: String? = null,
    ): ApiResponse<JointOperationHistoryItemDto>
}