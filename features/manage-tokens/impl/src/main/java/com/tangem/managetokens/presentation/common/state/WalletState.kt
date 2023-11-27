package com.tangem.managetokens.presentation.common.state

internal data class WalletState(
    val walletId: String,
    val artworkUrl: String?,
    val walletName: String,
    val onSelected: (String) -> Unit,
)