package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.quotes.multi.MultiQuoteStatusFetcher
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.multi.MultiYieldBalanceFetcher
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.repository.CurrenciesRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class FetchCardTokenListUseCase(
    private val currenciesRepository: CurrenciesRepository,
    private val multiNetworkStatusFetcher: MultiNetworkStatusFetcher,
    private val multiQuoteStatusFetcher: MultiQuoteStatusFetcher,
    private val multiYieldBalanceFetcher: MultiYieldBalanceFetcher,
    private val stakingIdFactory: StakingIdFactory,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, refresh: Boolean = false): Either<TokenListError, Unit> {
        return either {
            val currencies = fetchCurrencies(userWalletId = userWalletId, refresh = refresh)

            coroutineScope {
                val fetchStatuses = async {
                    fetchNetworksStatuses(
                        userWalletId = userWalletId,
                        networks = currencies.mapTo(destination = hashSetOf(), transform = CryptoCurrency::network),
                    )
                }
                val fetchQuotes = async {
                    fetchQuotes(
                        currenciesIds = currencies.mapNotNullTo(destination = hashSetOf()) { it.id.rawCurrencyId },
                    )
                }
                val yieldBalances = async {
                    fetchYieldBalances(
                        userWalletId = userWalletId,
                        currencies = currencies,
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
    ) {
        multiNetworkStatusFetcher(
            MultiNetworkStatusFetcher.Params(userWalletId = userWalletId, networks = networks),
        )
            .mapLeft(TokenListError::DataError)
            .bind()
    }

    private suspend fun fetchQuotes(currenciesIds: Set<CryptoCurrency.RawID>) {
        multiQuoteStatusFetcher(
            params = MultiQuoteStatusFetcher.Params(currenciesIds = currenciesIds, appCurrencyId = null),
        )
    }

    private suspend fun fetchYieldBalances(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        val stakingIds = currencies.mapNotNullTo(hashSetOf()) {
            stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = it).getOrNull()
        }

        multiYieldBalanceFetcher(
            params = MultiYieldBalanceFetcher.Params(userWalletId = userWalletId, stakingIds = stakingIds),
        )
    }
}