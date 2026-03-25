package com.tangem.domain.card

interface ScanFailsRequester {

    suspend fun show(source: Source): Result

    enum class Source { MAIN, SIGN_IN, SETTINGS, INTRO }

    sealed class Result {
        data object Dismissed : Result()
    }
}