package com.tangem.features.onramp.swap.entity.transformer

import com.tangem.features.onramp.swap.entity.ExchangeCardUM
import com.tangem.features.onramp.swap.entity.SwapSelectTokensUM
import com.tangem.features.onramp.swap.entity.SwapSelectTokensUMTransformer

/**
 * Transformer for removing selected "from" token
 *
[REDACTED_AUTHOR]
 */
internal object RemoveSelectedFromTokenTransformer : SwapSelectTokensUMTransformer {

    override fun transform(prevState: SwapSelectTokensUM): SwapSelectTokensUM {
        return prevState.copy(
            exchangeFrom = ExchangeCardUM.Empty(titleReference = prevState.exchangeFrom.titleReference),
            exchangeTo = ExchangeCardUM.Empty(titleReference = prevState.exchangeTo.titleReference),
        )
    }
}