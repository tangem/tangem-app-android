package com.tangem.domain.moneyaccount

interface MoneyAccountFeatureToggles {
    val isMoneyAccountsEnabled: Boolean
    val isMxnEnabled: Boolean
    val isYieldModeEnabled: Boolean
}