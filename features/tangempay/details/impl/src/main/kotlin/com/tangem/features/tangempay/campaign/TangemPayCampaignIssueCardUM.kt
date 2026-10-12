package com.tangem.features.tangempay.campaign

import androidx.compose.runtime.Immutable

@Immutable
internal data class TangemPayCampaignIssueCardUM(
    val cardArtUrl: String?,
    val issueFee: String,
    val isIssuing: Boolean,
    val onIssueClick: () -> Unit,
    val onCloseClick: () -> Unit,
)