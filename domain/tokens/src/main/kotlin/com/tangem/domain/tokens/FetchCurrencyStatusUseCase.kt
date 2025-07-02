package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.right
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.quotes.multi.MultiQuoteFetcher
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.staking.single.SingleYieldBalanceFetcher
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Use case responsible for fetching currency status information, including network status
 * and quotes for a given cryptocurrency. It provides methods to fetch currency status either
 * by providing a specific currency ID or fetching the status of the primary currency.
 *
 * @param currenciesRepository The repository for retrieving currency-related data.
 */
// TODO: Add tests
@Suppress("LongParameterList")
class FetchCurrencyStatusUseCase(
    private val currenciesRepository: CurrenciesRepository,
    private val stakingRepository: StakingRepository,
    private val singleNetworkStatusFetcher: SingleNetworkStatusFetcher,
    private val multiQuoteFetcher: MultiQuoteFetcher,
    private val singleYieldBalanceFetcher: SingleYieldBalanceFetcher,
    private val tokensFeatureToggles: TokensFeatureToggles,
) {

    /**
     * Fetches the status of a specific cryptocurrency for a given user wallet.
     *
     * @param userWalletId The ID of the user's wallet.
     * @param id The ID of the cryptocurrency.
     * @param refresh Indicates whether to force a refresh of the status data.
     * @return An [Either] representing success (Right) or an error (Left) in fetching the status.
     */
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        id: CryptoCurrency.ID,
        refresh: Boolean = false,
    ): Either<CurrencyStatusError, Unit> {
        return either {
            val currency = getCurrency(userWalletId, id)

            return@either coroutineScope {
                val fetchStatus = async {
                    fetchNetworkStatus(userWalletId = userWalletId, network = currency.network)
                }

                val fetchQuote = async { fetchQuote(currencyId = currency.id) }

                val fetchStakingBalance = async {
                    fetchStakingBalance(userWalletId = userWalletId, cryptoCurrency = currency, refresh = refresh)
                }

                awaitAll(fetchStatus, fetchQuote, fetchStakingBalance).summarizeResult()
            }
        }
    }

    /**
     * Fetches the status of the primary cryptocurrency for a given user wallet.
     *
     * @param userWalletId The ID of the user's wallet.
     * @param refresh Indicates whether to force a refresh of the status data.
     * @return An [Either] representing success (Right) or an error (Left) in fetching the status.
     */
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        refresh: Boolean = false,
    ): Either<CurrencyStatusError, Unit> {
        return either {
            val currency = getPrimaryCurrency(userWalletId, refresh)

            return@either coroutineScope {
                val fetchStatus = async {
                    fetchNetworkStatus(userWalletId = userWalletId, network = currency.network)
                }

                val fetchQuote = async { fetchQuote(currencyId = currency.id) }

                awaitAll(fetchStatus, fetchQuote).summarizeResult()
            }
        }
    }

    private suspend fun Raise<CurrencyStatusError>.getCurrency(
        userWalletId: UserWalletId,
        id: CryptoCurrency.ID,
    ): CryptoCurrency {
        return catch(
            block = { currenciesRepository.getMultiCurrencyWalletCurrency(userWalletId = userWalletId, id = id) },
        ) {
            raise(CurrencyStatusError.DataError(it))
        }
    }

    private suspend fun Raise<CurrencyStatusError>.getPrimaryCurrency(
        userWalletId: UserWalletId,
        refresh: Boolean = false,
    ): CryptoCurrency {
        return catch({ currenciesRepository.getSingleCurrencyWalletPrimaryCurrency(userWalletId, refresh) }) {
            raise(CurrencyStatusError.DataError(it))
        }
    }

    private suspend fun fetchNetworkStatus(userWalletId: UserWalletId, network: Network): Either<Throwable, Unit> {
        return singleNetworkStatusFetcher(
            params = SingleNetworkStatusFetcher.Params(userWalletId = userWalletId, network = network),
        )
    }

    private suspend fun fetchQuote(currencyId: CryptoCurrency.ID): Either<Throwable, Unit> {
        return multiQuoteFetcher(
            params = MultiQuoteFetcher.Params(
                currenciesIds = setOfNotNull(currencyId.rawCurrencyId),
                appCurrencyId = null,
            ),
        )
    }

    private suspend fun fetchStakingBalance(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
        refresh: Boolean,
    ): Either<Throwable, Unit> {
        return if (tokensFeatureToggles.isStakingLoadingRefactoringEnabled) {
            singleYieldBalanceFetcher(
                params = SingleYieldBalanceFetcher.Params(
                    userWalletId = userWalletId,
                    currencyId = cryptoCurrency.id,
                    network = cryptoCurrency.network,
                ),
            )
        } else {
            Either.catch { stakingRepository.fetchSingleYieldBalance(userWalletId, cryptoCurrency, refresh) }
        }
    }

    private fun List<Either<Throwable, Unit>>.summarizeResult(): Either<Throwable, Unit> {
        return firstOrNull { it.isLeft() } ?: Unit.right()
    }
}