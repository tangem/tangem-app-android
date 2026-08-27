package com.tangem.features.tangempay.card.issue

internal data class TangemPayIssueAdditionalCardUM(
    val isBalanceInsufficient: Boolean,
    val feeText: String,
    val isLoading: Boolean,
    val onIssueClick: () -> Unit,
    val onAddFundsClick: () -> Unit,
    val onDismiss: () -> Unit,
)