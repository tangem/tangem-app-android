package com.tangem.features.onramp.swap.entity

/**
 * Swap select tokens UI model
 *
 * @property onBackClick  callback is called when back button is clicked
 * @property exchangeFrom exchange "from" card UI model
 * @property exchangeTo   exchange "to" card UI model
 *
[REDACTED_AUTHOR]
 */
internal data class SwapSelectTokensUM(
    val onBackClick: () -> Unit,
    val exchangeFrom: ExchangeCardUM,
    val exchangeTo: ExchangeCardUM,
    val isBalanceHidden: Boolean,
)