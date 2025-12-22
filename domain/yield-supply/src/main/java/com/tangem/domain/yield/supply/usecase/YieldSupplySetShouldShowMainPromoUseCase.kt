package com.tangem.domain.yield.supply.usecase

import com.tangem.domain.yield.supply.YieldSupplyRepository

class YieldSupplySetShouldShowMainPromoUseCase(
    private val yieldSupplyRepository: YieldSupplyRepository,
) {

    suspend operator fun invoke(shouldShow: Boolean) {
        yieldSupplyRepository.setShouldShowYieldPromoBanner(shouldShow)
    }
}