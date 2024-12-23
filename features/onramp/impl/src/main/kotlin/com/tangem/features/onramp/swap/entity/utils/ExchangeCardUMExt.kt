package com.tangem.features.onramp.swap.entity.utils

import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.swap.entity.ExchangeCardUM

/** Create empty exchange "from" card */
internal fun createEmptyExchangeFrom(): ExchangeCardUM.Empty {
    return ExchangeCardUM.Empty(
        titleReference = resourceReference(id = R.string.swapping_from_title),
        subtitleReference = resourceReference(id = R.string.action_buttons_you_want_to_swap),
    )
}

/** Create empty exchange "to" card */
internal fun createEmptyExchangeTo(): ExchangeCardUM.Empty {
    return ExchangeCardUM.Empty(
        titleReference = resourceReference(id = R.string.swapping_to_title),
        subtitleReference = resourceReference(id = R.string.action_buttons_you_want_to_receive),
    )
}

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