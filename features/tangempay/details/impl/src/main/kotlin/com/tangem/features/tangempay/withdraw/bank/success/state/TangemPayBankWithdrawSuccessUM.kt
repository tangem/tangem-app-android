package com.tangem.features.tangempay.withdraw.bank.success.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

/** Closing screen of the wizard: what was sent, when it was sent and to whom. */
@Immutable
internal data class TangemPayBankWithdrawSuccessUM(
    val title: TextReference,
    val subtitle: TextReference,
    val rows: ImmutableList<RowUM>,
    val onCloseClick: () -> Unit,
) {

    @Immutable
    data class RowUM(
        val label: TextReference,
        val value: String,
    )
}