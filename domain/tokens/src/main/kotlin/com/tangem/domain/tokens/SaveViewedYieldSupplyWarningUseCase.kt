package com.tangem.domain.tokens

import com.tangem.domain.tokens.repository.YieldSupplyWarningsViewedRepository

class SaveViewedYieldSupplyWarningUseCase(
    private val yieldSupplyWarningsViewedRepository: YieldSupplyWarningsViewedRepository,
) {
    suspend operator fun invoke(symbol: String) {
        yieldSupplyWarningsViewedRepository.view(symbol)
    }
}