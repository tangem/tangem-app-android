package com.tangem.domain.swap.usecase

import arrow.core.Either
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.SwapErrorResolver
import com.tangem.domain.swap.SwapRepositoryV2
import com.tangem.domain.swap.models.SwapCryptoCurrency
import com.tangem.domain.swap.models.SwapCurrencies
import com.tangem.domain.swap.models.SwapCurrenciesGroup
import com.tangem.domain.swap.models.SwapPairModel
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet

/**
 * Get list of swap pairs
 */
class GetSwapPairsUseCase(
    private val swapRepositoryV2: SwapRepositoryV2,
    private val swapErrorResolver: SwapErrorResolver,
) {

    /**
     * @param userWallet selected user wallet
     * @param initialCurrency initial currency to swap (to or from)
     * @param cryptoCurrencyStatusList list of added cryptocurrencies
     * @param filterProviderTypes filters only specified provider types, if empty returns providers as is
     */
    suspend operator fun invoke(
        userWallet: UserWallet,
        initialCurrency: CryptoCurrency,
        cryptoCurrencyStatusList: List<CryptoCurrencyStatus>,
        filterProviderTypes: List<ExpressProviderType>,
    ) = Either.catch {
        val pairs = swapRepositoryV2.getPairs(
            userWallet = userWallet,
            initialCurrency = initialCurrency,
            cryptoCurrencyStatusList = cryptoCurrencyStatusList,
            filterProviderTypes = filterProviderTypes,
        )

        val fromGroup = pairs.groupPairs(
            initialCurrency = initialCurrency,
            filteringCurrency = { it.from },
            groupingCurrency = { it.to },
            cryptoCurrencyStatusList = cryptoCurrencyStatusList,
        )
        val toGroup = pairs.groupPairs(
            initialCurrency = initialCurrency,
            filteringCurrency = { it.to },
            groupingCurrency = { it.from },
            cryptoCurrencyStatusList = cryptoCurrencyStatusList,
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
        cryptoCurrencyStatusList: List<CryptoCurrencyStatus>,
    ): SwapCurrenciesGroup {
        val availableCryptoCurrencies = filter { pair -> filteringCurrency(pair).currency.id == initialCurrency.id }
            // Search available to swap currency
            .filter { pair ->
                cryptoCurrencyStatusList.any { currencyStatus ->
                    currencyStatus.currency.id == pair.to.currency.id
                }
            }.map { pair -> SwapCryptoCurrency(groupingCurrency(pair), pair.providers) }

        val unavailableCryptoCurrencies =
            cryptoCurrencyStatusList - availableCryptoCurrencies.map { it.currencyStatus }.toSet()

        return SwapCurrenciesGroup(
            available = availableCryptoCurrencies,
            unavailable = unavailableCryptoCurrencies.map { pair -> SwapCryptoCurrency(pair, emptyList()) },
            afterSearch = false,
        )
    }
}