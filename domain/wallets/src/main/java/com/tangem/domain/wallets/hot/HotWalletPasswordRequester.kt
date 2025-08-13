package com.tangem.domain.wallets.hot

import com.tangem.hot.sdk.model.HotAuth

interface HotWalletPasswordRequester {

    suspend fun wrongPassword()

    suspend fun successfulAuthentication()

    suspend fun requestPassword(hasBiometry: Boolean): Result

    suspend fun dismiss()

    sealed class Result {
        data object UseBiometry : Result()
        data object Dismiss : Result()
        data class EnteredPassword(val password: HotAuth.Password) : Result()
    }
}