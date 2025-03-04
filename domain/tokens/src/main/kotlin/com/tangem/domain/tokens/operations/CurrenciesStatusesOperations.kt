package com.tangem.domain.tokens.operations

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.core.utils.EitherFlow
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.tokens.model.Quote
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.*

class CurrenciesStatusesOperations(
    currenciesRepository: CurrenciesRepository,
    private val quotesRepository: QuotesRepository,
    private val networksRepository: NetworksRepository,
    stakingRepository: StakingRepository,
) : BaseCurrencyStatusOperations(currenciesRepository, quotesRepository, networksRepository, stakingRepository) {

    override fun getQuotes(id: CryptoCurrency.RawID): Flow<Either<Error, Set<Quote>>> {
        return quotesRepository.getQuotesUpdatesLegacy(setOf(id))
            .map<Set<Quote>, Either<Error, Set<Quote>>> { quotes ->
                if (quotes.isEmpty()) Error.EmptyQuotes.left() else quotes.right()
            }
            .retryWhen { cause, _ ->
                emit(Error.DataError(cause).left())
                true
            }
    }

    override fun getNetworksStatuses(
        userWalletId: UserWalletId,
        network: Network,
    ): EitherFlow<Error, Set<NetworkStatus>> {
        return networksRepository.getNetworkStatusesUpdatesLegacy(userWalletId, setOf(network))
            .map<Set<NetworkStatus>, Either<Error, Set<NetworkStatus>>> { it.right() }
            .catch { emit(Error.DataError(it).left()) }
            .onEmpty { emit(Error.EmptyNetworksStatuses.left()) }
    }

    override suspend fun fetchComponents(
        userWalletId: UserWalletId,
        networks: Set<Network>,
        currenciesIds: Set<CryptoCurrency.ID>,
        currencies: List<CryptoCurrency>,
    ): Either<Throwable, Unit> = Unit.right()

    sealed class Error {

        data object EmptyCurrencies : Error()

        data object EmptyQuotes : Error()

        data object EmptyNetworksStatuses : Error()

        data object EmptyAddresses : Error()

        data object UnableToCreateCurrencyStatus : Error()

        data class DataError(val cause: Throwable) : Error()

        data object EmptyYieldBalances : Error()
    }
}