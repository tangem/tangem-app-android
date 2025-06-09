package com.tangem.features.walletconnect.transaction.entity.common

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

internal data class WcTransactionRequestInfoUM(
    val blocks: ImmutableList<WcTransactionRequestBlockUM>,
    val onCopy: () -> Unit,
) : TangemBottomSheetConfigContent

internal data class WcTransactionRequestBlockUM(
    val info: ImmutableList<WcTransactionRequestInfoItemUM>,
)

internal data class WcTransactionRequestInfoItemUM(
    val title: TextReference,
    val description: String = "",
)