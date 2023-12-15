package com.tangem.managetokens.presentation.managetokens.viewmodels

import com.tangem.managetokens.presentation.common.state.NetworkItemState
import com.tangem.managetokens.presentation.managetokens.state.TokenItemState

internal interface ManageTokensClickIntents {
    fun onAddCustomTokensButtonClick()

    fun onSearchQueryChange(query: String)
    fun onSearchActiveChange(active: Boolean)

    fun onTokenItemButtonClick(token: TokenItemState.Loaded)

    fun onGenerateDerivationClick()

    fun onBackClick()

    fun onCloseChooseNetworkScreen()

    fun onNetworkToggleClick(token: TokenItemState.Loaded, network: NetworkItemState.Toggleable)
    fun onNonNativeNetworkHintClick()

    fun onSelectWalletsClick()

    fun onChooseWalletClick()

    fun onCloseChoosingWalletClick()

    fun onWalletSelected(walletId: String)
}