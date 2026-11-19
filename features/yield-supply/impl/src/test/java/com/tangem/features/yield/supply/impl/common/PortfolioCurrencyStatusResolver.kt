package com.tangem.features.yield.supply.impl.common

import com.tangem.domain.account.status.provider.NonPortfolioCurrenciesProvider
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow

/** A resolver over [supplier] for a wallet that holds nothing outside the portfolio. */
internal fun portfolioCurrencyStatusResolver(supplier: SingleAccountStatusListSupplier): YieldSupplyCurrencyStatusResolver {
    val nonPortfolioCurrenciesProvider: NonPortfolioCurrenciesProvider = mockk {
        coEvery { getStatuses(any(), any()) } returns emptyList()
        every { observe(any(), any()) } returns emptyFlow()
    }
    return YieldSupplyCurrencyStatusResolver(
        singleAccountStatusListSupplier = supplier,
        nonPortfolioCurrenciesProvider = nonPortfolioCurrenciesProvider,
    )
}