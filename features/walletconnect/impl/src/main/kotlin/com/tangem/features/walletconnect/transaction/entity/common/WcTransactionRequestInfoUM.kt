package com.tangem.features.walletconnect.transaction.entity.common

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class WcTransactionRequestInfoUM(
    val blocks: ImmutableList<WcTransactionRequestBlockUM>,
    val onCopy: () -> Unit,
) : TangemBottomSheetConfigContent

@Immutable
internal data class WcTransactionRequestBlockUM(
    val info: ImmutableList<WcTransactionRequestInfoItemUM>,
)

@Immutable
internal data class WcTransactionRequestInfoItemUM(
    val title: TextReference,
    val description: String = "",
)