package com.tangem.common.utils

import com.tangem.domain.balancehiding.BalanceHidingSettings
import com.tangem.domain.balancehiding.repositories.BalanceHidingRepository
import kotlinx.coroutines.runBlocking

fun BalanceHidingRepository.hideBalances() = setBalanceHidden(isBalanceHidden = true)

fun BalanceHidingRepository.showBalances() = setBalanceHidden(isBalanceHidden = false)

private fun BalanceHidingRepository.setBalanceHidden(isBalanceHidden: Boolean) = runBlocking {
    storeBalanceHidingSettings(
        BalanceHidingSettings(
            isHidingEnabledInSettings = true,
            isBalanceHidden = isBalanceHidden,
            isBalanceHidingNotificationEnabled = false,
        ),
    )
}