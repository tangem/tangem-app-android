package com.tangem.domain.yield.supply.usecase

import com.tangem.domain.yield.supply.YieldSupplyRepository
import kotlinx.coroutines.flow.Flow

class YieldSupplyGetShouldShowMainPromoUseCase(
    private val yieldSupplyRepository: YieldSupplyRepository,
) {

    operator fun invoke(): Flow<Boolean> {
        return yieldSupplyRepository.getShouldShowYieldPromoBanner()
    }
}