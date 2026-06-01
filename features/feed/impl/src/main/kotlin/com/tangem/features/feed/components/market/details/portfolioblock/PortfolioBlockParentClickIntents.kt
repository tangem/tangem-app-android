package com.tangem.features.feed.components.market.details.portfolioblock

import com.tangem.domain.models.currency.CryptoCurrency

internal interface PortfolioBlockParentClickIntents {
    fun openAddToPortfolioDirect()
    fun openAddToPortfolioViaUserPortfolio(rawCurrencyId: CryptoCurrency.RawID)
    fun openAddFunds(rawCurrencyId: CryptoCurrency.RawID)
}