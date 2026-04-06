package com.tangem.domain.tokens.model.tokensync

sealed class TokenSyncProgress {

    data object Idle : TokenSyncProgress()

    data class InProgress(
        val completedNetworks: Int,
        val totalNetworks: Int,
    ) : TokenSyncProgress() {
        val progressPercent: Int
            get() = if (totalNetworks > 0) {
                completedNetworks * 100 / totalNetworks
            } else {
                0
            }
    }

    data object Completed : TokenSyncProgress()

    data class Error(val cause: Throwable) : TokenSyncProgress()
}