package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.feature.wallet.presentation.wallet.state.model.WalletScreenState
import com.tangem.feature.wallet.presentation.wallet.state.util.TangemPayStateCreator.createIssueProgressState

internal class TangemPayIssueProgressStateTransformer : WalletScreenStateTransformer {

    override fun transform(prevState: WalletScreenState): WalletScreenState =
        prevState.copy(tangemPayState = createIssueProgressState())
}