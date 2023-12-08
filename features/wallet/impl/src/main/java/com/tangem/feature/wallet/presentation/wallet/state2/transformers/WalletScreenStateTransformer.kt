package com.tangem.feature.wallet.presentation.wallet.state2.transformers

import com.tangem.feature.wallet.presentation.wallet.state2.WalletScreenState

internal interface WalletScreenStateTransformer {

    fun transform(prevState: WalletScreenState): WalletScreenState
}