package com.tangem.domain.tokens.operations

import arrow.core.*
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.ensureNotNull
import arrow.core.raise.recover
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.core.lce.lce
import com.tangem.domain.core.lce.lceFlow
import com.tangem.domain.core.utils.EitherFlow
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.staking.model.stakekit.YieldBalanceList
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.model.*
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

internal class CurrenciesStatusesCachedOperations(
    private val currenciesRepository: CurrenciesRepository,
    private val quotesRepository: QuotesRepository,
    private val networksRepository: NetworksRepository,
    private val stakingRepository: StakingRepository,
) {

    fun getCurrenciesStatuses(userWalletId: UserWalletId): LceFlow<TokenListError, List<CryptoCurrencyStatus>> {
        return transformToCurrenciesStatuses(
            userWalletId = userWalletId,
            currenciesFlow = getCurrencies(userWalletId),
        )
    }

    private fun transformToCurrenciesStatuses(
        userWalletId: UserWalletId,
        currenciesFlow: EitherFlow<TokenListError, List<CryptoCurrency>>,
    ): LceFlow<TokenListError, List<CryptoCurrencyStatus>> = lceFlow {
        currenciesFlow.collectLatest { maybeCurrencies ->
            val isUpdating = MutableStateFlow(value = true)
            val nonEmptyCurrencies = maybeCurrencies.bind().toNonEmptyListOrNull()

            ensureNotNull(nonEmptyCurrencies) { TokenListError.EmptyTokens }

            val (networks, currenciesIds) = getIds(nonEmptyCurrencies)

            fun createCurrenciesStatuses(
                maybeQuotes: Either<TokenListError, Set<Quote>>?,
                maybeNetworkStatuses: Either<TokenListError, Set<NetworkStatus>>?,
                maybeYieldBalances: Either<TokenListError, YieldBalanceList>?,
                isUpdating: Boolean,
            ) = createCurrenciesStatuses(
                currencies = nonEmptyCurrencies,
                maybeQuotes = maybeQuotes,
                maybeNetworkStatuses = maybeNetworkStatuses,
                maybeYieldBalances = maybeYieldBalances,
                isUpdating = isUpdating,
            )

            combine(
                flow = getQuotes(currenciesIds),
                flow2 = getNetworksStatuses(userWalletId, networks),
                flow3 = getYieldBalances(userWalletId, nonEmptyCurrencies),
                flow4 = isUpdating,
                transform = ::createCurrenciesStatuses,
            )
                .distinctUntilChanged()
                .onEach { maybeCurrenciesStatuses ->
                    send(maybeCurrenciesStatuses)
                }
                .launchIn(scope = this)

            launch {
                fetchComponents(userWalletId, networks, currenciesIds, nonEmptyCurrencies)
            }.invokeOnCompletion {
                isUpdating.value = false
            }
        }
    }

    private suspend fun Raise<TokenListError>.fetchComponents(
        userWalletId: UserWalletId,
        networks: NonEmptySet<Network>,
        currenciesIds: NonEmptySet<CryptoCurrency.ID>,
        currencies: NonEmptyList<CryptoCurrency>,
    ) = coroutineScope {
        catch(
            block = {
                awaitAll(
                    async { networksRepository.fetchNetworkStatuses(userWalletId, networks) },
                    async {
                        val rawCurrenciesIds = currenciesIds.mapNotNullTo(mutableSetOf()) { it.rawCurrencyId }
                        quotesRepository.fetchQuotes(rawCurrenciesIds)
                    },
                    async { stakingRepository.fetchMultiYieldBalance(userWalletId, currencies) },
                )
            },
            catch = {
                raise(TokenListError.DataError(it))
            },
        )
    }

    private fun createCurrenciesStatuses(
        currencies: NonEmptyList<CryptoCurrency>,
        maybeQuotes: Either<TokenListError, Set<Quote>>?,
        maybeNetworkStatuses: Either<TokenListError, Set<NetworkStatus>>?,
        maybeYieldBalances: Either<TokenListError, YieldBalanceList>?,
        isUpdating: Boolean,
    ): Lce<TokenListError, List<CryptoCurrencyStatus>> = lce {
        isLoading.set(isUpdating)

        var quotesRetrievingFailed = false

        val networksStatuses = maybeNetworkStatuses?.bindEither()?.toNonEmptySetOrNull()
        val yieldBalances = maybeYieldBalances?.bindEither()
        val quotes = recover({ maybeQuotes?.bind()?.toNonEmptySetOrNull() }) {
            null
        }

        if (quotes == null) {
            quotesRetrievingFailed = true
        }

        currencies.map { currency ->
            val quote = quotes?.firstOrNull { it.rawCurrencyId == currency.id.rawCurrencyId }
            val networkStatus = networksStatuses?.firstOrNull { it.network == currency.network }
            val yieldBalance = findYieldBalanceOrNull(yieldBalances, currency, networkStatus)

            val currencyStatus = createCurrencyStatus(
                currency = currency,
                quote = quote,
                networkStatus = networkStatus,
                yieldBalance = yieldBalance,
                ignoreQuote = quotesRetrievingFailed,
            )

            currencyStatus
        }
    }

    private fun findYieldBalanceOrNull(
        yieldBalances: YieldBalanceList?,
        currency: CryptoCurrency,
        networkStatus: NetworkStatus?,
    ): YieldBalance? {
        if (yieldBalances !is YieldBalanceList.Data) return null

        val supportedIntegration = stakingRepository.getSupportedIntegrationId(currency.id)

        if (supportedIntegration.isNullOrBlank()) return null

        return yieldBalances.getBalance(
            address = extractAddress(networkStatus),
            integrationId = supportedIntegration,
        )
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

    private fun getCurrencies(userWalletId: UserWalletId): EitherFlow<TokenListError, List<CryptoCurrency>> {
        return currenciesRepository.getWalletCurrenciesUpdates(userWalletId)
            .map<List<CryptoCurrency>, Either<TokenListError, List<CryptoCurrency>>> { it.right() }
            .catch { emit(TokenListError.DataError(it).left()) }
            .distinctUntilChanged()
    }

    private fun getQuotes(tokensIds: NonEmptySet<CryptoCurrency.ID>): Flow<Either<TokenListError, Set<Quote>>> {
        return quotesRepository.getQuotesUpdates(tokensIds.mapNotNull { it.rawCurrencyId }.toSet())
            .map<Set<Quote>, Either<TokenListError, Set<Quote>>> { it.right() }
            .retryWhen { cause, _ ->
                emit(TokenListError.DataError(cause).left())
                // adding delay before retry to avoid spam when flow restarted
                delay(RETRY_DELAY)
                true
            }
            .distinctUntilChanged()
    }

    private fun getNetworksStatuses(
        userWalletId: UserWalletId,
        networks: NonEmptySet<Network>,
    ): EitherFlow<TokenListError, Set<NetworkStatus>> {
        return networksRepository.getNetworkStatusesUpdates(userWalletId, networks)
            .map<Set<NetworkStatus>, Either<TokenListError, Set<NetworkStatus>>> { it.right() }
            .retryWhen { cause, _ ->
                emit(TokenListError.DataError(cause).left())
                // adding delay before retry to avoid spam when flow restarted
                delay(RETRY_DELAY)
                true
            }
            .distinctUntilChanged()
    }

    private fun getYieldBalances(
        userWalletId: UserWalletId,
        cryptoCurrencies: List<CryptoCurrency>,
    ): EitherFlow<TokenListError, YieldBalanceList> {
        return stakingRepository.getMultiYieldBalanceUpdates(userWalletId, cryptoCurrencies)
            .map<YieldBalanceList, Either<TokenListError, YieldBalanceList>> { it.right() }
            .retryWhen { cause, _ ->
                emit(TokenListError.DataError(cause).left())
                // adding delay before retry to avoid spam when flow restarted
                delay(RETRY_DELAY)
                true
            }
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

    private fun extractAddress(networkStatus: NetworkStatus?): String? {
        return when (val value = networkStatus?.value) {
            is NetworkStatus.NoAccount -> value.address.defaultAddress.value
            is NetworkStatus.Unreachable -> value.address?.defaultAddress?.value
            is NetworkStatus.Verified -> value.address.defaultAddress.value
            else -> null
        }
    }

    companion object {
        private const val RETRY_DELAY = 2000L
    }
}