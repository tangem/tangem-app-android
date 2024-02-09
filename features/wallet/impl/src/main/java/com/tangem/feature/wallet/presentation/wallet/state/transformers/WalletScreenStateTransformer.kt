package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.feature.wallet.presentation.wallet.state.model.WalletScreenState

internal interface WalletScreenStateTransformer {

    fun transform(prevState: WalletScreenState): WalletScreenState
}