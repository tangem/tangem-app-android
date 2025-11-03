package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.models.YieldSupplyMarketChartData

class YieldSupplyGetChartUseCase(
    private val yieldSupplyRepository: YieldSupplyRepository,
) {

    suspend operator fun invoke(cryptoCurrency: CryptoCurrency.Token): Either<Throwable, YieldSupplyMarketChartData> =
        Either.catch {
            yieldSupplyRepository.getTokenChart(cryptoCurrency)
        }
}