package com.tangem.feature.swap.domain

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.swap.domain.api.SwapRepository
import com.tangem.feature.swap.domain.models.domain.LeastTokenInfo
import com.tangem.feature.swap.domain.models.domain.SwapPairLeast

class GetAvailablePairsUseCase(
    private val swapRepository: SwapRepository,
) {

    suspend operator fun invoke(
        userWallet: UserWallet,
        initialCurrency: LeastTokenInfo,
        currencies: List<CryptoCurrency>,
    ): List<SwapPairLeast> {
        return swapRepository.getPairsOnly(
            userWallet = userWallet,
            initialCurrency = initialCurrency,
            currencyList = currencies,
        ).pairs
    }
}