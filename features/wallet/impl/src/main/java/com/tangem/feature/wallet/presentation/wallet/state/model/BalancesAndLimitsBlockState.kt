package com.tangem.feature.wallet.presentation.wallet.state.model

import androidx.compose.runtime.Immutable

@Immutable
internal sealed class BalancesAndLimitsBlockState {

    object Loading : BalancesAndLimitsBlockState()

    object Error : BalancesAndLimitsBlockState()

    data class Content(
        val availableBalance: String,
        val limitDays: Int,
        val isEnabled: Boolean,
        val onClick: () -> Unit,
    ) : BalancesAndLimitsBlockState()
}