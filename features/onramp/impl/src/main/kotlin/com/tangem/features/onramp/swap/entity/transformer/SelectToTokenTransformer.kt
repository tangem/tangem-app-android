package com.tangem.features.onramp.swap.entity.transformer

import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.features.onramp.swap.entity.ExchangeCardUM
import com.tangem.features.onramp.swap.entity.SwapSelectTokensUM
import com.tangem.features.onramp.swap.entity.SwapSelectTokensUMTransformer
import com.tangem.features.onramp.swap.entity.utils.toFilled

/**
 * Transformer for selecting "to" token
 *
 * @property selectedTokenItemState token item state
 *
[REDACTED_AUTHOR]
 */
internal class SelectToTokenTransformer(
    private val selectedTokenItemState: TokenItemState,
) : SwapSelectTokensUMTransformer {

    override fun transform(prevState: SwapSelectTokensUM): SwapSelectTokensUM {
        return prevState.copy(
            exchangeFrom = prevState.exchangeFrom.hideRemoveButton(),
            exchangeTo = prevState.exchangeTo.toFilled(selectedTokenItemState = selectedTokenItemState),
        )
    }

    private fun ExchangeCardUM.hideRemoveButton(): ExchangeCardUM {
        return (this as? ExchangeCardUM.Filled)?.copy(removeButtonUM = null) ?: this
    }
}