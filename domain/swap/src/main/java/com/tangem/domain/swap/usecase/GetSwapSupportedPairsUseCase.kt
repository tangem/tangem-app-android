package com.tangem.domain.swap.usecase

import arrow.core.Either
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.swap.SwapErrorResolver
import com.tangem.domain.swap.SwapRepositoryV2
import com.tangem.domain.swap.models.*

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
        filterProviderTypes: List<ExpressProviderType>,
        swapTxType: SwapTxType,
    ) = Either.catch {
        // Request all provider types so the use case can tell apart currencies available for the current flow
        // from those available only in the regular Swap flow. The current-flow filter is applied below.
        val pairs = swapRepositoryV2.getSupportedPairs(
            userWallet = userWallet,
            initialCurrency = initialCurrency,
            cryptoCurrencyList = cryptoCurrencyList,
            filterProviderTypes = emptyList(),
            swapTxType = swapTxType,
        )

        val filteredOutInitial = cryptoCurrencyList.filterNot { currency ->
            currency.id.rawNetworkId == initialCurrency.id.rawNetworkId &&
                currency.id.rawCurrencyId == initialCurrency.id.rawCurrencyId
        }

        val fromGroup = pairs.groupPairs(
            initialCurrency = initialCurrency,
            filteringCurrency = { it.from },
            groupingCurrency = { it.to },
            cryptoCurrencyList = filteredOutInitial,
            allowedProviderTypes = filterProviderTypes,
        )
        val toGroup = pairs.groupPairs(
            initialCurrency = initialCurrency,
            filteringCurrency = { it.to },
            groupingCurrency = { it.from },
            cryptoCurrencyList = filteredOutInitial,
            allowedProviderTypes = filterProviderTypes,
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
        allowedProviderTypes: List<ExpressProviderType>,
    ): SwapCurrenciesGroup {
        val candidates = asSequence()
            .filter { pair ->
                filteringCurrency(pair).currency.id.rawCurrencyId == initialCurrency.id.rawCurrencyId
            }
            .filter { pair ->
                // Search available to swap currency
                cryptoCurrencyList.any { currencyStatus ->
                    currencyStatus.id == pair.to.currency.id
                }
            }
            .map { pair ->
                val toCurrency = groupingCurrency(pair)
                val isTxExtrasSupported = toCurrency.currency.network.transactionExtrasType.isTxExtrasSupported()
                // Providers eligible for the current flow (e.g. Send with Swap): extra-id support + allowed type
                val eligibleProviders = pair.providers
                    .filter { !isTxExtrasSupported || it.isExtraIdSupported }
                    .filter { allowedProviderTypes.isEmpty() || it.type in allowedProviderTypes }
                // The pair has any provider => it can still be swapped in the regular Swap flow
                SwapCryptoCurrency(toCurrency, eligibleProviders) to pair.providers.isNotEmpty()
            }
            .toList()

        val availableCryptoCurrencies = candidates
            .filter { (currency, _) -> currency.providers.isNotEmpty() }
            .map { (currency, _) -> currency }
        val availableCurrencies = availableCryptoCurrencies.map { it.currencyStatus.currency }.toSet()

        // Currencies with no providers for the current flow, but available in the regular Swap flow
        val availableForSwapCryptoCurrencies = candidates
            .filter { (currency, isAvailableForRegularSwap) ->
                currency.providers.isEmpty() && isAvailableForRegularSwap
            }
            .map { (currency, _) -> currency }
            .distinctBy { it.currencyStatus.currency }
            .filterNot { it.currencyStatus.currency in availableCurrencies }

        val usedCurrencies = availableCurrencies +
            availableForSwapCryptoCurrencies.map { it.currencyStatus.currency }.toSet()
        val unavailableCryptoCurrencies = cryptoCurrencyList - usedCurrencies

        return SwapCurrenciesGroup(
            available = availableCryptoCurrencies,
            unavailable = unavailableCryptoCurrencies.map { currency ->
                SwapCryptoCurrency(
                    CryptoCurrencyStatus(
                        currency = currency,
                        value = CryptoCurrencyStatus.Loading,
                    ),
                    emptyList(),
                )
            },
            isAfterSearch = false,
            availableForSwap = availableForSwapCryptoCurrencies,
        )
    }
}