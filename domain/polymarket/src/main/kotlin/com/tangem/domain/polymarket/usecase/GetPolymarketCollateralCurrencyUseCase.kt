package com.tangem.domain.polymarket.usecase

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.polymarket.PolymarketCollateralCurrencyFactory

class GetPolymarketCollateralCurrencyUseCase(
    private val collateralCurrencyFactory: PolymarketCollateralCurrencyFactory,
) {

    operator fun invoke(): CryptoCurrency.Token = collateralCurrencyFactory.create()
}