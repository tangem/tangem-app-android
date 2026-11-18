package com.tangem.features.yield.supply.impl.common

import com.tangem.domain.account.status.provider.NonPortfolioCurrenciesProvider
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import io.mockk.coEvery
import io.mockk.mockk

/** A resolver over [supplier] for a wallet that holds nothing outside the portfolio. */
internal fun portfolioCurrencyStatusResolver(supplier: SingleAccountStatusListSupplier): YieldSupplyCurrencyStatusResolver {
    val nonPortfolioCurrenciesProvider: NonPortfolioCurrenciesProvider = mockk {
        coEvery { findCurrency(any(), any()) } returns null
        coEvery { getStatuses(any(), any()) } returns emptyList()
    }
    return YieldSupplyCurrencyStatusResolver(
        singleAccountStatusListSupplier = supplier,
        nonPortfolioCurrenciesProvider = nonPortfolioCurrenciesProvider,
    )
}