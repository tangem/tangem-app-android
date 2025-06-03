package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.quotes.multi.MultiQuoteFetcher
import com.tangem.domain.staking.fetcher.YieldBalanceFetcherParams
import com.tangem.domain.staking.multi.MultiYieldBalanceFetcher
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

@Suppress("LongParameterList")
class FetchCardTokenListUseCase(
    private val currenciesRepository: CurrenciesRepository,
    private val networksRepository: NetworksRepository,
    private val quotesRepository: QuotesRepository,
    private val stakingRepository: StakingRepository,
    private val multiNetworkStatusFetcher: MultiNetworkStatusFetcher,
    private val multiQuoteFetcher: MultiQuoteFetcher,
    private val multiYieldBalanceFetcher: MultiYieldBalanceFetcher,
    private val tokensFeatureToggles: TokensFeatureToggles,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, refresh: Boolean = false): Either<TokenListError, Unit> {
        return either {
            val currencies = fetchCurrencies(userWalletId = userWalletId, refresh = refresh)

            coroutineScope {
                val fetchStatuses = async {
                    fetchNetworksStatuses(
                        userWalletId = userWalletId,
                        networks = currencies.mapTo(destination = hashSetOf(), transform = CryptoCurrency::network),
                        refresh = refresh,
                    )
                }
                val fetchQuotes = async {
                    fetchQuotes(
                        currenciesIds = currencies.mapNotNullTo(destination = hashSetOf()) { it.id.rawCurrencyId },
                        refresh = refresh,
                    )
                }
                val yieldBalances = async {
                    fetchYieldBalances(
                        userWalletId = userWalletId,
                        currencies = currencies,
                        refresh = refresh,
                    )
                }
                awaitAll(fetchStatuses, fetchQuotes, yieldBalances)
            }
        }
    }

    private suspend fun Raise<TokenListError>.fetchCurrencies(
        userWalletId: UserWalletId,
        refresh: Boolean = false,
    ): List<CryptoCurrency> {
        return catch(
            block = {
                currenciesRepository.getSingleCurrencyWalletWithCardCurrencies(
                    userWalletId = userWalletId,
                    refresh = refresh,
                )
            },
            catch = { raise(TokenListError.DataError(it)) },
        )
    }

    private suspend fun Raise<TokenListError>.fetchNetworksStatuses(
        userWalletId: UserWalletId,
        networks: Set<Network>,
        refresh: Boolean,
    ) {
        if (tokensFeatureToggles.isNetworksLoadingRefactoringEnabled) {
            multiNetworkStatusFetcher(
                MultiNetworkStatusFetcher.Params(userWalletId = userWalletId, networks = networks),
            )
                .mapLeft { TokenListError.DataError(it) }
        } else {
            catch(
                block = { networksRepository.getNetworkStatusesSync(userWalletId, networks, refresh) },
                catch = { raise(TokenListError.DataError(it)) },
            )
        }
    }

    private suspend fun fetchQuotes(currenciesIds: Set<CryptoCurrency.RawID>, refresh: Boolean) {
        if (tokensFeatureToggles.isQuotesLoadingRefactoringEnabled) {
            multiQuoteFetcher(
                params = MultiQuoteFetcher.Params(currenciesIds = currenciesIds, appCurrencyId = null),
            )
        } else {
            catch(
                block = { quotesRepository.getQuotesSync(currenciesIds, refresh) },
                catch = { /* Ignore error */ },
            )
        }
    }

    private suspend fun fetchYieldBalances(
        userWalletId: UserWalletId,
        currencies: List<CryptoCurrency>,
        refresh: Boolean,
    ) {
        if (tokensFeatureToggles.isStakingLoadingRefactoringEnabled) {
            multiYieldBalanceFetcher(
                params = YieldBalanceFetcherParams.Multi(
                    userWalletId = userWalletId,
                    currencyIdWithNetworkMap = currencies.associateTo(hashMapOf()) { it.id to it.network },
                ),
            )
        } else {
            catch(
                block = { stakingRepository.fetchMultiYieldBalance(userWalletId, currencies, refresh) },
                catch = { /* Ignore error */ },
            )
        }
    }
}