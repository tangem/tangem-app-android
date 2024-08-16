package com.tangem.domain.tokens.operations

import arrow.core.*
import arrow.core.raise.recover
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.core.lce.lce
import com.tangem.domain.core.utils.lceError
import com.tangem.domain.core.utils.lceLoading
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.staking.model.stakekit.YieldBalanceList
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.model.*
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

internal class CurrenciesStatusesLceOperations(
    private val currenciesRepository: CurrenciesRepository,
    private val quotesRepository: QuotesRepository,
    private val networksRepository: NetworksRepository,
    private val stakingRepository: StakingRepository,
) {

    fun getCurrenciesStatuses(
        userWalletId: UserWalletId,
        isSingleCurrencyWalletsAllowed: Boolean = false,
    ): LceFlow<TokenListError, List<CryptoCurrencyStatus>> {
        return transformToCurrenciesStatuses(
            userWalletId = userWalletId,
            flow = if (isSingleCurrencyWalletsAllowed) {
                getWalletCurrencies(userWalletId)
            } else {
                getMultiCurrencyWalletCurrencies(userWalletId)
            },
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun transformToCurrenciesStatuses(
        userWalletId: UserWalletId,
        flow: LceFlow<TokenListError, List<CryptoCurrency>>,
    ): LceFlow<TokenListError, List<CryptoCurrencyStatus>> {
        return flow.transformLatest transform@{ maybeCurrencies ->
            val nonEmptyCurrencies = maybeCurrencies.fold(
                ifLoading = { maybeContent ->
                    emit(createLoadingCurrenciesStatuses(maybeContent))
                    return@transform
                },
                ifContent = { content ->
                    val nonEmptyCurrencies = content.toNonEmptyListOrNull()

                    if (nonEmptyCurrencies == null) {
                        emit(TokenListError.EmptyTokens.lceError())
                        return@transform
                    } else {
                        nonEmptyCurrencies
                    }
                },
                ifError = { error ->
                    emit(error.lceError())
                    return@transform
                },
            )

            val (networks, currenciesIds) = getIds(nonEmptyCurrencies)

            val addresses = networksRepository.getNetworkAddresses(userWalletId)
            combine(
                getQuotes(currenciesIds),
                getNetworksStatuses(userWalletId, networks),
                getYieldBalances(userWalletId, addresses),
            ) { maybeQuotes, maybeNetworksStatuses, maybeYieldBalances ->
                val statuses = createCurrenciesStatuses(
                    currencies = nonEmptyCurrencies,
                    maybeQuotes = maybeQuotes,
                    maybeNetworkStatuses = maybeNetworksStatuses,
                    maybeYieldBalances = maybeYieldBalances,
                )
                emit(statuses)
            }.collect()
        }
    }

    private fun createLoadingCurrenciesStatuses(
        maybeCurrencies: List<CryptoCurrency>?,
    ): Lce<TokenListError, List<CryptoCurrencyStatus>> {
        val nonEmptyCurrencies = maybeCurrencies?.toNonEmptyListOrNull()

        val statuses = if (nonEmptyCurrencies == null) {
            lceLoading()
        } else {
            createCurrenciesStatuses(
                currencies = nonEmptyCurrencies,
                maybeNetworkStatuses = null,
                maybeQuotes = null,
                maybeYieldBalances = null,
            )
        }

        return statuses
    }

    private fun getWalletCurrencies(userWalletId: UserWalletId): LceFlow<TokenListError, List<CryptoCurrency>> {
        return currenciesRepository.getWalletCurrenciesUpdates(userWalletId)
            .map { maybeCurrencies ->
                maybeCurrencies.mapError { TokenListError.DataError(it) }
            }
    }

    private fun getMultiCurrencyWalletCurrencies(
        userWalletId: UserWalletId,
    ): LceFlow<TokenListError, List<CryptoCurrency>> {
        return currenciesRepository.getMultiCurrencyWalletCurrenciesUpdatesLce(userWalletId)
            .map { maybeCurrencies ->
                maybeCurrencies.mapError { TokenListError.DataError(it) }
            }
    }

    private fun createCurrenciesStatuses(
        currencies: NonEmptyList<CryptoCurrency>,
        maybeQuotes: Either<TokenListError, Set<Quote>>?,
        maybeNetworkStatuses: Lce<TokenListError, Set<NetworkStatus>>?,
        maybeYieldBalances: Lce<TokenListError, YieldBalanceList>?,
    ): Lce<TokenListError, List<CryptoCurrencyStatus>> = lce {
        isLoading.set(maybeNetworkStatuses == null)

        var quotesRetrievingFailed = false

        val networksStatuses = maybeNetworkStatuses?.bindOrNull()?.toNonEmptySetOrNull()
        val quotes = recover({ maybeQuotes?.bind()?.toNonEmptySetOrNull() }) {
            quotesRetrievingFailed = true
            null
        }?.ifEmpty {
            quotesRetrievingFailed = true
            null
        }

        val yieldBalances = maybeYieldBalances?.getOrNull()

        currencies.map { currency ->
            val quote = quotes?.firstOrNull { it.rawCurrencyId == currency.id.rawCurrencyId }
            val networkStatus = networksStatuses?.firstOrNull { it.network == currency.network }
            val yieldBalance = (yieldBalances as? YieldBalanceList.Data)?.getBalance(
                rawCurrencyId = currency.id.rawCurrencyId,
                networkName = currency.network.name,
            )

            createCurrencyStatus(
                currency = currency,
                quote = quote,
                networkStatus = networkStatus,
                yieldBalance = yieldBalance,
                ignoreQuote = quotesRetrievingFailed,
            )
        }
    }

    private fun createCurrencyStatus(
        currency: CryptoCurrency,
        quote: Quote?,
        networkStatus: NetworkStatus?,
        yieldBalance: YieldBalance?,
        ignoreQuote: Boolean,
    ): CryptoCurrencyStatus {
        val currencyStatusOperations = CurrencyStatusOperations(
            currency = currency,
            quote = quote,
            networkStatus = networkStatus,
            yieldBalance = yieldBalance,
            ignoreQuote = ignoreQuote,
        )

        return currencyStatusOperations.createTokenStatus()
    }

    private fun getQuotes(tokensIds: NonEmptySet<CryptoCurrency.ID>): Flow<Either<TokenListError, Set<Quote>>> {
        return quotesRepository.getQuotesUpdates(tokensIds)
            .map<Set<Quote>, Either<TokenListError, Set<Quote>>> { it.right() }
            .catch { emit(TokenListError.DataError(it).left()) }
    }

    private fun getNetworksStatuses(
        userWalletId: UserWalletId,
        networks: NonEmptySet<Network>,
    ): LceFlow<TokenListError, Set<NetworkStatus>> {
        return networksRepository.getNetworkStatusesUpdatesLce(userWalletId, networks)
            .map { maybeStatuses ->
                maybeStatuses.mapError { TokenListError.DataError(it) }
            }
    }

    private fun getYieldBalances(
        userWalletId: UserWalletId,
        addresses: List<CryptoCurrencyAddress>,
    ): LceFlow<TokenListError, YieldBalanceList> {
        return stakingRepository.getMultiYieldBalanceLce(
            userWalletId = userWalletId,
            addresses = addresses,
        ).map { maybeBalances ->
            maybeBalances.mapError { TokenListError.DataError(it) }
        }
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
