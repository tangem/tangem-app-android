package com.tangem.features.walletconnect.transaction.entity.approve

import androidx.annotation.DrawableRes
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent

internal data class WcCustomAllowanceUM(
    @DrawableRes val networkIconRes: Int,
    val tokenIconUrl: String,
    val amountText: String,
    val isUnlimited: Boolean,
) : TangemBottomSheetConfigContent