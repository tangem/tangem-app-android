package com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer

import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsUM
import com.tangem.utils.transformer.Transformer

/**
 * Wires the balance block's "Add funds" button click handlers to the provided actions.
 *
 * [TokenDetailsStateController.getInitialState] sets up the button without click handlers
 * because the controller can't see [TokenDetailsClickIntents]; this transformer fills them in
 * once the model is constructed.
 */
internal class BindAddFundsActionButtonTransformer(
    private val onClick: () -> Unit,
    private val onLongClick: () -> Unit,
) : Transformer<TokenDetailsUM> {

    override fun transform(prevState: TokenDetailsUM): TokenDetailsUM {
        val prev = prevState.balanceBlockUM
        val updated = prev.addFundsButton.copy(onClick = onClick, onLongClick = onLongClick)
        return prevState.copy(balanceBlockUM = prev.copyButtons(addFundsButton = updated))
    }
}