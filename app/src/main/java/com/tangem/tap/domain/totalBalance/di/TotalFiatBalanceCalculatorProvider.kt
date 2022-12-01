package com.tangem.tap.domain.totalBalance.di

import com.tangem.tap.domain.totalBalance.TotalFiatBalanceCalculator
import com.tangem.tap.domain.totalBalance.implementation.DefaultTotalFiatBalanceCalculator

fun TotalFiatBalanceCalculator.Companion.provideDefaultImplementation(): TotalFiatBalanceCalculator {
    return DefaultTotalFiatBalanceCalculator()
}
