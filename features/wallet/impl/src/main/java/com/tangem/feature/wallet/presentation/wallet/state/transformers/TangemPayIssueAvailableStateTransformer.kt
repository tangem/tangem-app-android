package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.feature.wallet.presentation.wallet.state.model.WalletScreenState
import com.tangem.feature.wallet.presentation.wallet.state.util.TangemPayStateCreator.createIssueAvailableState

internal class TangemPayIssueAvailableStateTransformer(
    private val onClickIssue: () -> Unit = {},
) : WalletScreenStateTransformer {

    override fun transform(prevState: WalletScreenState): WalletScreenState =
        prevState.copy(tangemPayState = createIssueAvailableState(onClickIssue))
}