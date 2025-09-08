package com.tangem.features.yieldlending.impl.subcomponents.active.entity

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.TextReference

internal data class YieldLendingActiveUM(
    val bottomSheetConfig: TangemBottomSheetConfig,
)

internal sealed class YieldLendingActiveContentUM : TangemBottomSheetConfigContent {
    data class Main(
        val totalEarnings: TextReference,
        val availableBalance: TextReference,
    ) : YieldLendingActiveContentUM()

    data object StopEarning : YieldLendingActiveContentUM()
}