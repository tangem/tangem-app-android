package com.tangem.domain.wallets.hot

import com.tangem.hot.sdk.model.HotAuth
import com.tangem.hot.sdk.model.HotWalletId
import java.util.UUID

/**
 * Interface for requesting the password for a hot wallet.
 * It provides methods to handle password requests, authentication states, and user interactions.
 */
interface HotWalletPasswordRequester {

    /**
     * Sets state to show wrong password state.
     *
     * The result is attributed to [attemptRequest] (the request that produced it), not to whatever
     * request currently owns the dialog. This prevents a late callback of one request from
     * incrementing the failed-attempt counter of a different wallet that meanwhile replaced it.
     */
    suspend fun wrongPassword(attemptRequest: AttemptRequest)

    /**
     * Sets state to show successful authentication state.
     *
     * Attributed to [attemptRequest], see [wrongPassword].
     */
    suspend fun successfulAuthentication(attemptRequest: AttemptRequest)

    /**
     * Requests the user to enter the password for the hot wallet.
     * @param attemptRequest Contains information about the hot wallet and authentication mode.
     * @return Result of the password request, which can be either a password entry, biometric use, or dismissal.
     */
    suspend fun requestPassword(attemptRequest: AttemptRequest): Result

    /**
     * Dismisses the password request dialog.
     */
    suspend fun dismiss()

    /**
     * Represents a request to authenticate with a hot wallet.
     * @param hotWalletId The ID of the hot wallet to authenticate with.
     * @param authMode Indicates whether the request is for authentication mode.
     * In auth mode user can be deleted after failed attempts.
     * @param hasBiometry Indicates whether to show biometric authentication option to the user.
     * Will be ignored if the device does not support biometry at the moment of the request.
     * @param requestId Unique identity of this request, used to bind async result callbacks
     * (wrong/successful) back to the exact request that produced them.
     */
    data class AttemptRequest(
        val hotWalletId: HotWalletId,
        val authMode: Boolean,
        val hasBiometry: Boolean,
        val requestId: String = UUID.randomUUID().toString(),
    )

    sealed class Result {
        data object UseBiometry : Result()
        data object Dismiss : Result()
        data class EnteredPassword(val password: HotAuth.Password) : Result()
    }
}