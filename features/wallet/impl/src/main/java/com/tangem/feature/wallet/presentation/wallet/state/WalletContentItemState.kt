package com.tangem.feature.wallet.presentation.wallet.state

import com.tangem.feature.wallet.presentation.common.state.TokenItemState

/**
 * @author Andrew Khokhlov on 20/06/2023
 */
internal sealed interface WalletContentItemState {

    data class NetworkGroupTitle(val networkName: String) : WalletContentItemState

    data class Token(val tokenItemState: TokenItemState) : WalletContentItemState
}
