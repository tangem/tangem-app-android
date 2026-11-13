package com.tangem.features.earn.impl.incentive.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal data class EarnIncentiveSheetUM(
    val title: TextReference,
    val footerTitle: TextReference,
    val footerDescription: TextReference,
    val onDismiss: () -> Unit,
) : TangemBottomSheetConfigContent