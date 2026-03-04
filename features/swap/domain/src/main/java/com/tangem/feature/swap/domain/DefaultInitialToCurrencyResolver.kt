package com.tangem.feature.swap.domain

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.swap.domain.models.ui.AccountSwapCurrency
import com.tangem.feature.swap.domain.models.ui.TokensDataStateExpress
import com.tangem.feature.swap.domain.models.ui.getGroupWithReverse
import com.tangem.utils.extensions.orZero

internal class DefaultInitialToCurrencyResolver(
    private val swapTransactionRepository: SwapTransactionRepository,
) : InitialToCurrencyResolver {

    override suspend fun tryGetFromCache(
        userWallet: UserWallet,
        initialCryptoCurrency: CryptoCurrency,
        state: TokensDataStateExpress,
        isReverseFromTo: Boolean,
    ): AccountSwapCurrency? {
        val id = swapTransactionRepository.getLastSwappedCryptoCurrencyId(userWallet.walletId) ?: return null

        return if (id != initialCryptoCurrency.id.value) {
            val group = state.getGroupWithReverse(isReverseFromTo)

            group.accountCurrencyList.firstNotNullOfOrNull { (_, currencyList) ->
                currencyList.find { it.isAvailable && it.cryptoCurrencyStatus.currency.id.value == id }
            }
        } else {
            null
        }
    }

    override fun tryGetWithMaxAmount(state: TokensDataStateExpress, isReverseFromTo: Boolean): AccountSwapCurrency? {
        val group = state.getGroupWithReverse(isReverseFromTo)
        return group.accountCurrencyList.firstNotNullOfOrNull { (_, currencyList) ->
            currencyList.maxByOrNull { swapAccountCurrency ->
                swapAccountCurrency.cryptoCurrencyStatus.value.fiatAmount
                    .takeIf { swapAccountCurrency.isAvailable }
                    .orZero()
            }
        }
    }
}