package com.tangem.domain.wallets.hot

import com.tangem.hot.sdk.model.HotAuth
import com.tangem.hot.sdk.model.HotWalletId

/**
 * Interface for requesting the password for a hot wallet.
 * It provides methods to handle password requests, authentication states, and user interactions.
 */
interface HotWalletPasswordRequester {

    /**
     * Sets state to show wrong password state.
     */
    suspend fun wrongPassword()

    /**
     * Sets state to show successful authentication state.
     */
    suspend fun successfulAuthentication()

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
     * @param hasBiometry Indicates whether to show biometric authentication option.
     */
    data class AttemptRequest(
        val hotWalletId: HotWalletId,
        val authMode: Boolean,
        val hasBiometry: Boolean,
    )

    sealed class Result {
        data object UseBiometry : Result()
        data object Dismiss : Result()
        data class EnteredPassword(val password: HotAuth.Password) : Result()
    }
}