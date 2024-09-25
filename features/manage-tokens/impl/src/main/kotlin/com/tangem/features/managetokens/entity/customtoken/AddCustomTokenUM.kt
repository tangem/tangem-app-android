package com.tangem.features.managetokens.entity.customtoken

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.TextReference

internal data class AddCustomTokenUM(
    val popBack: () -> Unit,
    val title: TextReference,
    val showBackButton: Boolean,
) : TangemBottomSheetConfigContent