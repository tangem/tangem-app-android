package com.tangem.features.markets.details.impl.ui.state

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.TextReference

internal data class InfoBottomSheetContent(
    val title: TextReference,
    val body: TextReference,
    val generatedAINotificationUM: GeneratedAINotificationUM? = null,
) : TangemBottomSheetConfigContent {

    data class GeneratedAINotificationUM(val onClick: () -> Unit)
}