package com.tangem.features.walletconnect.transaction.entity.approve

import com.tangem.core.ui.extensions.TextReference

internal data class WcSpendAllowanceUM(
    val amountText: TextReference,
    val tokenSymbol: String,
    val tokenImageUrl: String?,
)