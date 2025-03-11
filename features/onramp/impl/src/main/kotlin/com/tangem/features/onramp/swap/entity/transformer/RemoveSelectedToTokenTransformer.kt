package com.tangem.features.onramp.swap.entity.transformer

import com.tangem.features.onramp.swap.entity.ExchangeCardUM
import com.tangem.features.onramp.swap.entity.SwapSelectTokensUM
import com.tangem.features.onramp.swap.entity.SwapSelectTokensUMTransformer
import com.tangem.features.onramp.swap.entity.utils.createEmptyExchangeTo

/**
 * Transformer for removing selected "to" token
 *
[REDACTED_AUTHOR]
 */
internal class RemoveSelectedToTokenTransformer(
    private val onRemoveFromTokenClick: () -> Unit,
) : SwapSelectTokensUMTransformer {

    override fun transform(prevState: SwapSelectTokensUM): SwapSelectTokensUM {
        return prevState.copy(
            exchangeFrom = prevState.exchangeFrom.showRemoveButton(onClick = onRemoveFromTokenClick),
            exchangeTo = createEmptyExchangeTo(),
        )
    }

    private fun ExchangeCardUM.showRemoveButton(onClick: () -> Unit): ExchangeCardUM {
        return (this as? ExchangeCardUM.Filled)
            ?.copy(removeButtonUM = ExchangeCardUM.RemoveButtonUM(onClick = onClick))
            ?: this
    }
}