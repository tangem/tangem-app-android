package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.error.CurrencyStatusError
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
 * Use case responsible for fetching currency status information, including network status
 * and quotes for a given cryptocurrency. It provides methods to fetch currency status either
 * by providing a specific currency ID or fetching the status of the primary currency.
 *
 * @param currenciesRepository The repository for retrieving currency-related data.
 * @param networksRepository The repository for retrieving network-related data.
 * @param quotesRepository The repository for retrieving cryptocurrency quotes.
 */
// [REDACTED_TODO_COMMENT]
class FetchCurrencyStatusUseCase(
    private val currenciesRepository: CurrenciesRepository,
    private val networksRepository: NetworksRepository,
    private val quotesRepository: QuotesRepository,
    private val stakingRepository: StakingRepository,
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

            fetchCurrencyStatus(userWalletId, currency, refresh)
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
            val currency = getPrimaryCurrency(userWalletId)

            fetchCurrencyStatus(userWalletId, currency, refresh)
        }
    }

    private suspend fun Raise<CurrencyStatusError>.fetchCurrencyStatus(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        refresh: Boolean,
    ) = coroutineScope {
        val fetchStatus = async {
            fetchNetworkStatus(userWalletId, currency.network, refresh)
        }
        val fetchQuote = async {
            fetchQuote(currency.id, refresh)
        }
        val fetchStakingBalance = async {
            fetchStakingBalance(userWalletId, currency, refresh)
        }

        awaitAll(fetchStatus, fetchQuote, fetchStakingBalance)
    }

    private suspend fun Raise<CurrencyStatusError>.getCurrency(
        userWalletId: UserWalletId,
        id: CryptoCurrency.ID,
    ): CryptoCurrency {
        return catch(
            block = {
                currenciesRepository.getMultiCurrencyWalletCurrency(userWalletId, id)
            },
        ) {
            raise(CurrencyStatusError.DataError(it))
        }
    }

    private suspend fun Raise<CurrencyStatusError>.getPrimaryCurrency(userWalletId: UserWalletId): CryptoCurrency {
        return catch({ currenciesRepository.getSingleCurrencyWalletPrimaryCurrency(userWalletId) }) {
            raise(CurrencyStatusError.DataError(it))
        }
    }

    private suspend fun Raise<CurrencyStatusError>.fetchNetworkStatus(
        userWalletId: UserWalletId,
        network: Network,
        refresh: Boolean,
    ) {
        catch(
            block = { networksRepository.getNetworkStatusesSync(userWalletId, setOf(network), refresh) },
        ) {
            raise(CurrencyStatusError.DataError(it))
        }
    }

    private suspend fun Raise<CurrencyStatusError>.fetchQuote(currencyId: CryptoCurrency.ID, refresh: Boolean) {
        catch(
            block = { quotesRepository.getQuotesSync(setOf(currencyId), refresh) },
        ) {
            raise(CurrencyStatusError.DataError(it))
        }
    }

    private suspend fun Raise<CurrencyStatusError>.fetchStakingBalance(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
        refresh: Boolean,
    ) {
        catch(
            block = { stakingRepository.fetchSingleYieldBalance(userWalletId, cryptoCurrency, refresh) },
        ) {
            raise(CurrencyStatusError.DataError(it))
        }
    }
}
