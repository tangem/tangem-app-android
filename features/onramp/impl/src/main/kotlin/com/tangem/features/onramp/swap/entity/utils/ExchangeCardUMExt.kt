package com.tangem.features.onramp.swap.entity.utils

import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.features.onramp.swap.entity.ExchangeCardUM

/**
 * Convert from [ExchangeCardUM] to [ExchangeCardUM.Filled]
 *
 * @param selectedTokenItemState token item state
 * @param removeButtonUM         remove button UI model
 */
internal fun ExchangeCardUM.toFilled(
    selectedTokenItemState: TokenItemState,
    removeButtonUM: ExchangeCardUM.RemoveButtonUM? = null,
): ExchangeCardUM.Filled {
    return ExchangeCardUM.Filled(
        titleReference = titleReference,
        tokenItemState = selectedTokenItemState,
        removeButtonUM = removeButtonUM,
    )
}