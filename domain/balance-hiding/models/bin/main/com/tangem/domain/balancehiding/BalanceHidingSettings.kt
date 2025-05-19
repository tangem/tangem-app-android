package com.tangem.domain.balancehiding

data class BalanceHidingSettings(
    val isHidingEnabledInSettings: Boolean,
    val isBalanceHidden: Boolean,
    val isBalanceHidingNotificationEnabled: Boolean,
    val isUpdateFromToast: Boolean = false,
)