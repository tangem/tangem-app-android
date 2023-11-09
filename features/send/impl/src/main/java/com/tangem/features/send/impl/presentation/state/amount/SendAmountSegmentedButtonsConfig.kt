package com.tangem.features.send.impl.presentation.state.amount

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.currency.tokenicon.TokenIconState
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
internal data class SendAmountSegmentedButtonsConfig(
    val title: TextReference,
    val iconState: TokenIconState? = null,
    val iconUrl: String? = null,
    val isFiat: Boolean,
)