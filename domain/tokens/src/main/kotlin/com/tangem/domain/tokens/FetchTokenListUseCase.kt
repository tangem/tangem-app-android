package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.toNonEmptyListOrNull
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.domain.tokens.models.Network
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
 */
// TODO: Add tests
class FetchTokenListUseCase(
    private val currenciesRepository: CurrenciesRepository,
    private val networksRepository: NetworksRepository,
    private val quotesRepository: QuotesRepository,
) {

    /**
     * Fetches the token list information for a user's wallet, including currency data,
     * network statuses, and quotes for associated tokens.
     *
     * @param userWalletId The ID of the user's wallet.
     * @param refresh Indicates whether to force a refresh of the token list data.
     * @return An [Either] representing success (Right) or an error (Left) in fetching the token list.
     */
    suspend operator fun invoke(userWalletId: UserWalletId, refresh: Boolean = false): Either<TokenListError, Unit> {
        return either {
            val currencies = fetchCurrencies(userWalletId, refresh)

            coroutineScope {
                val fetchStatuses = async {
                    fetchNetworksStatuses(
                        userWalletId,
                        currencies.mapTo(hashSetOf()) { it.network },
                        refresh,
                    )
                }
                val fetchQuotes = async {
                    fetchQuotes(
                        currencies.mapTo(hashSetOf()) { it.id },
                        refresh,
                    )
                }

                awaitAll(fetchStatuses, fetchQuotes)
            }
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
        catch(
            block = { networksRepository.getNetworkStatusesSync(userWalletId, networks, refresh) },
        ) {
            raise(TokenListError.DataError(it))
        }
    }

    private suspend fun Raise<TokenListError>.fetchQuotes(currenciesIds: Set<CryptoCurrency.ID>, refresh: Boolean) {
        catch(
            block = { quotesRepository.getQuotesSync(currenciesIds, refresh) },
        ) {
            raise(TokenListError.DataError(it))
        }
    }
}