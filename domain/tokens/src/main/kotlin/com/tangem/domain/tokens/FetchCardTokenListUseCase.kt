package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
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

class FetchCardTokenListUseCase(
    private val currenciesRepository: CurrenciesRepository,
    private val networksRepository: NetworksRepository,
    private val quotesRepository: QuotesRepository,
    private val stakingRepository: StakingRepository,
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
        catch(
            block = { networksRepository.getNetworkStatusesSync(userWalletId, networks, refresh) },
            catch = { raise(TokenListError.DataError(it)) },
        )
    }

    private suspend fun fetchQuotes(currenciesIds: Set<CryptoCurrency.RawID>, refresh: Boolean) {
        catch(
            block = { quotesRepository.getQuotesSync(currenciesIds, refresh) },
            catch = { /* Ignore error */ },
        )
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
}