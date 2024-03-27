package com.tangem.domain.tokens.operations

import arrow.core.*
import arrow.core.raise.recover
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.core.lce.lce
import com.tangem.domain.core.utils.lceError
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.model.*
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.*

internal class CurrenciesStatusesLceOperations(
    private val currenciesRepository: CurrenciesRepository,
    private val quotesRepository: QuotesRepository,
    private val networksRepository: NetworksRepository,
) {

    fun getCurrenciesStatuses(userWalletId: UserWalletId): LceFlow<TokenListError, List<CryptoCurrencyStatus>> {
        return getMultiCurrencyWalletCurrencies(userWalletId).transform transform@{ maybeCurrencies ->
            val nonEmptyCurrencies = maybeCurrencies.fold(
                ifLoading = { maybeContent ->
                    maybeContent?.toNonEmptyListOrNull() ?: return@transform
                },
                ifContent = { content ->
                    content.toNonEmptyListOrNull() ?: return@transform
                },
                ifError = { error ->
                    emit(error.lceError())
                    return@transform
                },
            )

            val maybeLoadingCurrenciesStatuses = createCurrenciesStatuses(
                currencies = nonEmptyCurrencies,
                maybeNetworkStatuses = null,
                maybeQuotes = null,
            )

            emit(maybeLoadingCurrenciesStatuses)

            val (networks, currenciesIds) = getIds(nonEmptyCurrencies)

            val currenciesFlow = combine(
                getQuotes(currenciesIds),
                getNetworksStatuses(userWalletId, networks),
            ) { maybeQuotes, maybeNetworksStatuses ->
                createCurrenciesStatuses(nonEmptyCurrencies, maybeQuotes, maybeNetworksStatuses)
            }

            emitAll(currenciesFlow)
        }
    }

    private fun getMultiCurrencyWalletCurrencies(
        userWalletId: UserWalletId,
    ): LceFlow<TokenListError, List<CryptoCurrency>> {
        return currenciesRepository.getMultiCurrencyWalletCurrenciesUpdatesLce(userWalletId)
            .map { maybeCurrencies ->
                maybeCurrencies.mapError { TokenListError.DataError(it) }
            }
            .onEmpty<Lce<TokenListError, List<CryptoCurrency>>> { emit(TokenListError.EmptyTokens.lceError()) }
            .distinctUntilChanged()
    }

    private fun createCurrenciesStatuses(
        currencies: List<CryptoCurrency>,
        maybeQuotes: Either<TokenListError, Set<Quote>>?,
        maybeNetworkStatuses: Lce<TokenListError, Set<NetworkStatus>>?,
    ): Lce<TokenListError, List<CryptoCurrencyStatus>> = lce {
        isLoading.set(maybeNetworkStatuses == null)

        var quotesRetrievingFailed = false

        val networksStatuses = maybeNetworkStatuses?.bind()?.toNonEmptySetOrNull()
        val quotes = recover({ maybeQuotes?.bind()?.toNonEmptySetOrNull() }) {
            quotesRetrievingFailed = true
            null
        }

        currencies.map { currency ->
            val quote = quotes?.firstOrNull { it.rawCurrencyId == currency.id.rawCurrencyId }
            val networkStatus = networksStatuses?.firstOrNull { it.network == currency.network }

            createCurrencyStatus(currency, quote, networkStatus, ignoreQuote = quotesRetrievingFailed)
        }
    }

    private fun createCurrencyStatus(
        currency: CryptoCurrency,
        quote: Quote?,
        networkStatus: NetworkStatus?,
        ignoreQuote: Boolean,
    ): CryptoCurrencyStatus {
        val currencyStatusOperations = CurrencyStatusOperations(
            currency = currency,
            quote = quote,
            networkStatus = networkStatus,
            ignoreQuote = ignoreQuote,
        )

        return currencyStatusOperations.createTokenStatus()
    }

    private fun getQuotes(tokensIds: NonEmptySet<CryptoCurrency.ID>): Flow<Either<TokenListError, Set<Quote>>> {
        return quotesRepository.getQuotesUpdates(tokensIds)
            .map<Set<Quote>, Either<TokenListError, Set<Quote>>> { quotes ->
                if (quotes.isEmpty()) TokenListError.EmptyTokens.left() else quotes.right()
            }
            .catch { emit(TokenListError.DataError(it).left()) }
            .distinctUntilChanged()
    }

    private fun getNetworksStatuses(
        userWalletId: UserWalletId,
        networks: NonEmptySet<Network>,
    ): LceFlow<TokenListError, Set<NetworkStatus>> {
        return networksRepository.getNetworkStatusesUpdatesLce(userWalletId, networks)
            .map { maybeStatuses ->
                maybeStatuses.mapError { TokenListError.DataError(it) }
            }
            .onEmpty<Lce<TokenListError, Set<NetworkStatus>>> { emit(TokenListError.EmptyTokens.lceError()) }
            .distinctUntilChanged()
    }

    private fun getIds(currencies: List<CryptoCurrency>): Pair<NonEmptySet<Network>, NonEmptySet<CryptoCurrency.ID>> {
        val currencyIdToNetworkId = currencies.associate { currency ->
            currency.id to currency.network
        }
        val currenciesIds = currencyIdToNetworkId.keys.toNonEmptySetOrNull()
        val networks = currencyIdToNetworkId.values.toNonEmptySetOrNull()

        requireNotNull(currenciesIds) { "Currencies IDs cannot be empty" }
        requireNotNull(networks) { "Networks IDs cannot be empty" }

        return networks to currenciesIds
    }
}
