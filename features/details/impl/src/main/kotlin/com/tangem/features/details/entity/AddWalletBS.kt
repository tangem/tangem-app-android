package com.tangem.features.details.entity

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent

internal data class AddWalletBS(
    val onAddHardwareWalletClick: () -> Unit,
    val onAddMobileWalletClick: () -> Unit,
) : TangemBottomSheetConfigContent