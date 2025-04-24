package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.toNonEmptyListOrNull
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.quotes.multi.MultiQuoteFetcher
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

/**
 * Use case responsible for fetching token list information, including currency data,
 * network statuses, and quotes for tokens associated with a user's wallet.
 *
 * @param currenciesRepository The repository for retrieving currency-related data.
 * @param networksRepository The repository for retrieving network-related data.
 * @param quotesRepository The repository for retrieving cryptocurrency quotes.
 * @param stakingRepository The repository for retrieving staking-related data.
 */
// TODO: Add tests
@Suppress("LongParameterList")
class FetchTokenListUseCase(
    private val currenciesRepository: CurrenciesRepository,
    private val networksRepository: NetworksRepository,
    private val quotesRepository: QuotesRepository,
    private val stakingRepository: StakingRepository,
    private val multiNetworkStatusFetcher: MultiNetworkStatusFetcher,
    private val multiQuoteFetcher: MultiQuoteFetcher,
    private val tokensFeatureToggles: TokensFeatureToggles,
) {

    /**
     * Fetches the token list information for a user's wallet, including currency data,
     * network statuses, and quotes for associated tokens.
     *
     * @param userWalletId The ID of the user's wallet.
     * @param mode The refresh mode to control the fetching process.
     * @return An [Either] representing success (Right) or an error (Left) in fetching the token list.
     */
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        mode: RefreshMode = RefreshMode.NONE,
    ): Either<TokenListError, Unit> = either {
        val currencies = fetchCurrencies(userWalletId, refresh = mode.refreshCurrencies)

        invoke(userWalletId = userWalletId, currencies = currencies, mode = mode)
    }

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        currencies: List<CryptoCurrency>,
        mode: RefreshMode = RefreshMode.NONE,
    ): Either<TokenListError, Unit> = either {
        coroutineScope {
            val fetchStatuses = async {
                fetchNetworksStatuses(
                    userWalletId = userWalletId,
                    networks = currencies.mapTo(hashSetOf()) { it.network },
                    refresh = mode.refreshNetworksStatuses,
                )
            }
            val fetchQuotes = async {
                fetchQuotes(
                    currenciesIds = currencies.mapTo(hashSetOf()) { it.id },
                    refresh = mode.refreshQuotes,
                )
            }

            val yieldBalances = async {
                fetchYieldBalances(
                    userWalletId = userWalletId,
                    currencies = currencies,
                    refresh = mode.refreshYieldBalances,
                )
            }

            awaitAll(fetchStatuses, fetchQuotes, yieldBalances)
        }
    }

    private suspend fun Raise<TokenListError>.fetchCurrencies(
        userWalletId: UserWalletId,
        refresh: Boolean,
    ): List<CryptoCurrency> {
        val currencies = catch(
            block = { currenciesRepository.getMultiCurrencyWalletCurrenciesSync(userWalletId, refresh) },
        ) {
            raise(TokenListError.DataError(it))
        }

        return ensureNotNull(currencies.toNonEmptyListOrNull()) {
            TokenListError.EmptyTokens
        }
    }

    private suspend fun Raise<TokenListError>.fetchNetworksStatuses(
        userWalletId: UserWalletId,
        networks: Set<Network>,
        refresh: Boolean,
    ) {
        if (tokensFeatureToggles.isNetworksLoadingRefactoringEnabled) {
            if (refresh) {
                multiNetworkStatusFetcher(
                    params = MultiNetworkStatusFetcher.Params(userWalletId, networks),
                )
                    .mapLeft { TokenListError.DataError(it) }
                    .bind()
            }
        } else {
            catch(
                block = { networksRepository.getNetworkStatusesSync(userWalletId, networks, refresh) },
            ) {
                raise(TokenListError.DataError(it))
            }
        }
    }

    private suspend fun fetchQuotes(currenciesIds: Set<CryptoCurrency.ID>, refresh: Boolean) {
        if (tokensFeatureToggles.isQuotesLoadingRefactoringEnabled) {
            multiQuoteFetcher(
                params = MultiQuoteFetcher.Params(
                    currenciesIds = currenciesIds.mapNotNull { it.rawCurrencyId }.toSet(),
                    appCurrencyId = null,
                ),
            )
        } else {
            catch(
                block = {
                    val rawIds = currenciesIds.mapNotNull { it.rawCurrencyId }.toSet()
                    quotesRepository.getQuotesSync(rawIds, refresh)
                },
            ) {
                /* Ignore error */
            }
        }
    }

    private suspend fun fetchYieldBalances(
        userWalletId: UserWalletId,
        currencies: List<CryptoCurrency>,
        refresh: Boolean,
    ) {
        catch(
            block = { stakingRepository.fetchMultiYieldBalance(userWalletId, currencies, refresh) },
            catch = { /* Ignore error */ },
        )
    }

    /**
     * Represents the refresh modes available for fetching token list information.
     */
    enum class RefreshMode(
        internal val refreshCurrencies: Boolean,
        internal val refreshNetworksStatuses: Boolean,
        internal val refreshQuotes: Boolean,
        internal val refreshYieldBalances: Boolean,
    ) {
        NONE(
            refreshCurrencies = false,
            refreshNetworksStatuses = false,
            refreshQuotes = false,
            refreshYieldBalances = false,
        ),
        FULL(
            refreshCurrencies = true,
            refreshNetworksStatuses = true,
            refreshQuotes = true,
            refreshYieldBalances = true,
        ),
        SKIP_CURRENCIES(
            refreshCurrencies = false,
            refreshNetworksStatuses = true,
            refreshQuotes = true,
            refreshYieldBalances = true,
        ),
    }
}