package com.tangem.features.feed.earn.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import kotlinx.collections.immutable.ImmutableList

/**
 * @property scopeTabs which set of networks [networks] lists: every earn network, or the user's own ones
 * @property networks the "All" option of the selected scope followed by every network it covers
 * @property footer Reset/Apply buttons, present exactly while the draft differs from the applied filter
 */
internal data class EarnFilterByNetworkBottomSheetContentUM(
    val scopeTabs: ImmutableList<TangemTabItemUM>,
    val networks: ImmutableList<EarnFilterNetworkUM>,
    val footer: EarnFilterFooterUM?,
) : TangemBottomSheetConfigContent

@Immutable
internal data class EarnFilterFooterUM(
    val onReset: () -> Unit,
    val onApply: () -> Unit,
)