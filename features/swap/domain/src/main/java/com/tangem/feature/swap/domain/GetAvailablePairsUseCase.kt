package com.tangem.feature.swap.domain

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.feature.swap.domain.api.SwapRepository
import com.tangem.feature.swap.domain.models.domain.LeastTokenInfo
import com.tangem.feature.swap.domain.models.domain.SwapPairLeast

class GetAvailablePairsUseCase(
    private val swapRepository: SwapRepository,
) {

    suspend operator fun invoke(
        initialCurrency: LeastTokenInfo,
        currencies: List<CryptoCurrency>,
    ): List<SwapPairLeast> {
        return swapRepository.getPairsOnly(initialCurrency, currencies).pairs
    }
}