package com.tangem.features.hotwallet

import com.tangem.hot.sdk.model.HotAuth

interface HotWalletPasswordRequester {

    suspend fun wrongPassword()

    suspend fun requestPassword(hasBiometry: Boolean): Result

    suspend fun dismiss()

    sealed class Result {
        data object UseBiometry : Result()
        data object Dismiss : Result()
        data class EnteredPassword(val password: HotAuth.Password) : Result()
    }
}