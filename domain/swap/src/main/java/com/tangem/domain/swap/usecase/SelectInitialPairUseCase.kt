package com.tangem.domain.swap.usecase

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.SwapTransactionRepository
import com.tangem.domain.swap.models.SwapCurrencies
import com.tangem.domain.swap.models.SwapCurrenciesGroup
import com.tangem.domain.swap.models.SwapDirection
import com.tangem.domain.swap.models.getGroupWithReverse
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.utils.extensions.orZero

/**
 * Select initial pair to swap
 */
class SelectInitialPairUseCase(
    private val swapTransactionRepository: SwapTransactionRepository,
) {

    /**
     * @param userWallet selected user wallet
     * @param primaryCryptoCurrency initial currency
     * @param secondaryCryptoCurrency selected currency
     * @param swapCurrencies list of currencies with providers
     * @param swapDirection swap direction
     */
    suspend operator fun invoke(
        userWallet: UserWallet,
        primaryCryptoCurrency: CryptoCurrency,
        secondaryCryptoCurrency: CryptoCurrency?,
        swapCurrencies: SwapCurrencies,
        swapDirection: SwapDirection,
    ): CryptoCurrencyStatus? {
        val swapCurrenciesGroup = swapCurrencies.getGroupWithReverse(swapDirection)
        return tryToGetAlreadySelectedCurrency(secondaryCryptoCurrency, swapCurrenciesGroup)
            ?: tryGetFromCache(userWallet, primaryCryptoCurrency, swapCurrenciesGroup)
            ?: tryGetWithMaxAmount(swapCurrenciesGroup)
            ?: swapCurrenciesGroup.available.firstOrNull()?.currencyStatus
    }

    private fun tryToGetAlreadySelectedCurrency(
        secondaryCryptoCurrency: CryptoCurrency?,
        swapCurrenciesGroup: SwapCurrenciesGroup,
    ): CryptoCurrencyStatus? {
        return secondaryCryptoCurrency?.let {
            swapCurrenciesGroup.available.firstOrNull {
                secondaryCryptoCurrency.id == it.currencyStatus.currency.id
            }?.currencyStatus
        }
    }

    private suspend fun tryGetFromCache(
        userWallet: UserWallet,
        primaryCryptoCurrency: CryptoCurrency,
        swapCurrenciesGroup: SwapCurrenciesGroup,
    ): CryptoCurrencyStatus? {
        val id = swapTransactionRepository.getLastSwappedCryptoCurrencyId(userWallet.walletId) ?: return null

        return if (id != primaryCryptoCurrency.id.value) {
            swapCurrenciesGroup.available.find { it.currencyStatus.currency.id.value == id }?.currencyStatus
        } else {
            null
        }
    }

    private fun tryGetWithMaxAmount(swapCurrenciesGroup: SwapCurrenciesGroup): CryptoCurrencyStatus? {
        return swapCurrenciesGroup.available.maxByOrNull {
            it.currencyStatus.value.fiatAmount.orZero()
        }?.currencyStatus
    }
}