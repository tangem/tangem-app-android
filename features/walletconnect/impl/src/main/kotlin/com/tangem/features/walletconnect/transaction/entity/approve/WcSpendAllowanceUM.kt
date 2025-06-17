package com.tangem.features.walletconnect.transaction.entity.approve

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.TextReference
import java.math.BigDecimal

internal data class WcSpendAllowanceUM(
    val amountValue: BigDecimal,
    val isUnlimited: Boolean,
    val amountText: TextReference,
    val tokenSymbol: String,
    val tokenImageUrl: String?,
    val networkIconRes: Int?,
) : TangemBottomSheetConfigContent