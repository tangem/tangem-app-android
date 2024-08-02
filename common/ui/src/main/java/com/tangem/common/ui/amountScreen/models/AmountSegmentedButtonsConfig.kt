package com.tangem.common.ui.amountScreen.models

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference

/**
 * Segmented buttons config
 *
 * @param title button title
 * @param iconState currency icon state
 * @param iconUrl currency icon url
 * @param isFiat is fiat currency
 */
@Immutable
data class AmountSegmentedButtonsConfig(
    val title: TextReference,
    val iconState: CurrencyIconState? = null,
    val iconUrl: String? = null,
    val isFiat: Boolean,
)
