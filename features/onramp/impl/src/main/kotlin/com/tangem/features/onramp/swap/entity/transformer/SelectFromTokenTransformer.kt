package com.tangem.features.onramp.swap.entity.transformer

import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.features.onramp.swap.entity.ExchangeCardUM
import com.tangem.features.onramp.swap.entity.SwapSelectTokensUM
import com.tangem.features.onramp.swap.entity.SwapSelectTokensUMTransformer
import com.tangem.features.onramp.swap.entity.utils.toFilled

/**
 * Transformer for selecting "from" token
 *
 * @property selectedTokenItemState token item state
 * @property onRemoveClick          callback is called when remove button is clicked
 *
[REDACTED_AUTHOR]
 */
internal class SelectFromTokenTransformer(
    private val selectedTokenItemState: TokenItemState,
    private val onRemoveClick: () -> Unit,
) : SwapSelectTokensUMTransformer {

    override fun transform(prevState: SwapSelectTokensUM): SwapSelectTokensUM {
        return prevState.copy(
            exchangeFrom = prevState.exchangeFrom.toFilled(
                selectedTokenItemState = selectedTokenItemState,
                removeButtonUM = ExchangeCardUM.RemoveButtonUM(onClick = onRemoveClick),
            ),
        )
    }
}