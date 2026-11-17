package com.tangem.features.moneyaccount.main.ui.state

internal data class MoneyAccountActivationUM(
    val isColdWallet: Boolean,
    val isActivationInProgress: Boolean,
    val onActivateClick: () -> Unit,
    val onTermsClick: () -> Unit,
    val onDismiss: () -> Unit,
)