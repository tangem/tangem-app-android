package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.models.YieldChartWindow
import com.tangem.domain.yield.supply.models.YieldSupplyMarketChartData

class YieldSupplyGetChartUseCase(
    private val yieldSupplyRepository: YieldSupplyRepository,
) {

    suspend operator fun invoke(
        cryptoCurrency: CryptoCurrency.Token,
        window: YieldChartWindow? = null,
        bucketSizeDays: Int? = null,
    ): Either<Throwable, YieldSupplyMarketChartData> = Either.catch {
        yieldSupplyRepository.getTokenChart(cryptoCurrency, window, bucketSizeDays)
    }
}