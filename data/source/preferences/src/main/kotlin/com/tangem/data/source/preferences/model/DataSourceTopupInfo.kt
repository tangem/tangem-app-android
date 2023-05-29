package com.tangem.data.source.preferences.model

data class DataSourceTopupInfo(
    val walletId: String,
    val cardBalanceState: CardBalanceState,
) {
    enum class CardBalanceState(val serializedName: String) {
        Empty(serializedName = "Empty"),
        Full(serializedName = "Full"),
        CustomToken(serializedName = "Custom token"),
        BlockchainError(serializedName = "Blockchain error"),
    }
}