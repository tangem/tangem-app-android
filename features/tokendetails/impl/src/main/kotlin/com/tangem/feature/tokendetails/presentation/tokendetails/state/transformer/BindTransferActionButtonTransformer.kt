package com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer

import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsUM
import com.tangem.utils.transformer.Transformer

/**
 * Wires the balance block's "Transfer" button onClick to the provided action.
 *
 * See [BindAddFundsActionButtonTransformer] for the same pattern used for the "Add funds" button.
 */
internal class BindTransferActionButtonTransformer(
    private val onClick: () -> Unit,
) : Transformer<TokenDetailsUM> {

    override fun transform(prevState: TokenDetailsUM): TokenDetailsUM {
        val prev = prevState.balanceBlockUM
        val updated = prev.transferButton.copy(onClick = onClick)
        return prevState.copy(balanceBlockUM = prev.copyButtons(transferButton = updated))
    }
}