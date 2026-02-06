package com.tangem.features.feed.ui.earn.state

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import kotlinx.collections.immutable.ImmutableList

internal data class EarnFilterByNetworkBottomSheetContentUM(
    val selectedNetwork: EarnFilterNetworkUM,
    val networks: ImmutableList<EarnFilterNetworkUM>,
    val onOptionClick: (EarnFilterNetworkUM) -> Unit,
) : TangemBottomSheetConfigContent