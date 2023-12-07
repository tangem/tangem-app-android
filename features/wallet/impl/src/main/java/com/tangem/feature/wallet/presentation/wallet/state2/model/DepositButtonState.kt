package com.tangem.feature.wallet.presentation.wallet.state2.model

internal data class DepositButtonState(
    val isEnabled: Boolean,
    val onClick: () -> Unit,
)