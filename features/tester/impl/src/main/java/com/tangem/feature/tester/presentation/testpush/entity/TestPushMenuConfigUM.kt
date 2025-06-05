package com.tangem.feature.tester.presentation.testpush.entity

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import kotlinx.collections.immutable.ImmutableList

internal data class TestPushMenuConfigUM(
    val itemList: ImmutableList<PushMenu>,
    val onItemClick: (PushMenu) -> Unit,
) : TangemBottomSheetConfigContent {
    enum class PushMenu {
        MarketTokenDetails,
    }
}