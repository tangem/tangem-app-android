package com.tangem.features.tangempay.addfunds.bankTransferOnramp.details

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.tangempay.addfunds.bankTransferOnramp.fees.TangemPayOnrampFeesUM
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class TangemPayBankTransferOnrampDetailsScreenUM(
    val tabs: ImmutableList<TangemTabItemUM>,
    val requisites: ImmutableList<RequisiteUM>,
    val referenceWarning: TextReference?,
    val fees: TangemPayOnrampFeesUM,
    val dailyDepositLimit: DailyDepositLimitUM?,
    val onShareClick: () -> Unit,
    val onCloseClick: () -> Unit,
) {

    @Immutable
    data class RequisiteUM(
        val key: String,
        val title: TextReference,
        val value: String,
        val onCopyClick: () -> Unit,
    )

    @Immutable
    data class DailyDepositLimitUM(
        val value: TextReference,
        val hint: TextReference?,
    )
}