package com.tangem.domain.swap.usecase

import arrow.core.Either
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.SwapErrorResolver
import com.tangem.domain.swap.SwapRepositoryV2
import com.tangem.domain.swap.models.SwapCryptoCurrency
import com.tangem.domain.swap.models.SwapCurrencies
import com.tangem.domain.swap.models.SwapCurrenciesGroup
import com.tangem.domain.swap.models.SwapPairModel
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet

/**
 * Returns pais
 */
class GetSwapSupportedPairsUseCase(
    private val swapRepositoryV2: SwapRepositoryV2,
    private val swapErrorResolver: SwapErrorResolver,
) {

    suspend operator fun invoke(
        userWallet: UserWallet,
        initialCurrency: CryptoCurrency,
        cryptoCurrencyList: List<CryptoCurrency>,
    ) = Either.catch {
        val pairs = swapRepositoryV2.getPairsOnly(
            userWallet = userWallet,
            initialCurrency = initialCurrency,
            cryptoCurrencyList = cryptoCurrencyList,
        )

        val filteredOutInitial = cryptoCurrencyList.filterNot { it.id == initialCurrency.id }

        val fromGroup = pairs.groupPairs(
            initialCurrency = initialCurrency,
            filteringCurrency = { it.from },
            groupingCurrency = { it.to },
            cryptoCurrencyList = filteredOutInitial,
        )
        val toGroup = pairs.groupPairs(
            initialCurrency = initialCurrency,
            filteringCurrency = { it.to },
            groupingCurrency = { it.from },
            cryptoCurrencyList = filteredOutInitial,
        )

        SwapCurrencies(
            fromGroup = fromGroup,
            toGroup = toGroup,
        )
    }.mapLeft(swapErrorResolver::resolve)

    private fun List<SwapPairModel>.groupPairs(
        initialCurrency: CryptoCurrency,
        filteringCurrency: (SwapPairModel) -> CryptoCurrencyStatus,
        groupingCurrency: (SwapPairModel) -> CryptoCurrencyStatus,
        cryptoCurrencyList: List<CryptoCurrency>,
    ): SwapCurrenciesGroup {
        val availableCryptoCurrencies = filter { pair -> filteringCurrency(pair).currency.id == initialCurrency.id }
            // Search available to swap currency
            .filter { pair ->
                cryptoCurrencyList.any { currencyStatus ->
                    currencyStatus.id == pair.to.currency.id
                }
            }.map { pair -> SwapCryptoCurrency(groupingCurrency(pair), pair.providers) }

        val unavailableCryptoCurrencies =
            cryptoCurrencyList - availableCryptoCurrencies.map { it.currencyStatus.currency }.toSet()

        return SwapCurrenciesGroup(
            available = availableCryptoCurrencies,
            unavailable = unavailableCryptoCurrencies.map { currency ->
                SwapCryptoCurrency(
                    CryptoCurrencyStatus(
                        currency = currency,
                        value = CryptoCurrencyStatus.Loading, // todo select token unavailable
                    ),
                    emptyList(),
                )
            },
            afterSearch = false,
        )
    }
}