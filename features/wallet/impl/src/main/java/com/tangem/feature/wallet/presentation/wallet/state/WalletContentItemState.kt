package com.tangem.feature.wallet.presentation.wallet.state

import com.tangem.feature.wallet.presentation.common.state.TokenItemState

/**
[REDACTED_AUTHOR]
 */
internal sealed interface WalletContentItemState {

    data class NetworkGroupTitle(val networkName: String) : WalletContentItemState

    data class Token(val tokenItemState: TokenItemState) : WalletContentItemState
}