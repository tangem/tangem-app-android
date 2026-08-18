package com.tangem.features.tangempay.orderCard.impl.ui.state

import androidx.compose.runtime.Immutable

@Immutable
internal data class TangemPayOrderCardSuccessScreenUM(
    val deliveryEtaMaxBusinessDays: Int,
    val email: String,
    val onShowCardClick: () -> Unit,
)