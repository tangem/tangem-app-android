package com.tangem.features.tangempay.entity

internal data class TangemPayIssueAdditionalCardUM(
    val isBalanceInsufficient: Boolean,
    val feeText: String,
    val isLoading: Boolean,
    val onIssueClick: () -> Unit,
    val onAddFundsClick: () -> Unit,
    val onDismiss: () -> Unit,
)