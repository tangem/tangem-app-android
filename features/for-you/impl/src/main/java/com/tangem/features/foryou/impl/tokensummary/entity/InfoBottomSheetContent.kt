package com.tangem.features.foryou.impl.tokensummary.entity

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.TextReference

/**
 * Content of the informational bottom sheet shown when the user taps an indicator's info icon on the token
 * summary screen. Displays a [title] and an explanatory [body].
 */
internal data class InfoBottomSheetContent(
    val title: TextReference,
    val body: TextReference,
) : TangemBottomSheetConfigContent