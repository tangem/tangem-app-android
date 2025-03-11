package com.tangem.feature.wallet.presentation.wallet.state.model

internal data class DepositButtonState(
    val isEnabled: Boolean,
    val onClick: () -> Unit,
)