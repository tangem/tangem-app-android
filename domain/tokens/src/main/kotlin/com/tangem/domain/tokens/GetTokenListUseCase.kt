package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.left
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.error.mapper.mapToTokenListError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.tokens.operations.CurrenciesStatusesOperations
import com.tangem.domain.tokens.operations.TokenListOperations
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

class GetTokenListUseCase(
    internal val currenciesRepository: CurrenciesRepository,
    internal val quotesRepository: QuotesRepository,
    internal val networksRepository: NetworksRepository,
    internal val dispatchers: CoroutineDispatcherProvider,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(userWalletId: UserWalletId, refresh: Boolean = false): Flow<Either<TokenListError, TokenList>> {
        return getTokensStatuses(userWalletId, refresh).flatMapMerge { maybeTokens ->
            maybeTokens.fold(
                ifLeft = { error ->
                    flowOf(error.left())
                },
                ifRight = { tokens ->
                    createTokenList(userWalletId, tokens)
                },
            )
        }
    }

    private fun getTokensStatuses(
        userWalletId: UserWalletId,
        refresh: Boolean,
    ): Flow<Either<TokenListError, List<CryptoCurrencyStatus>>> {
        val operations = CurrenciesStatusesOperations(
            userWalletId = userWalletId,
            refresh = refresh,
            useCase = this@GetTokenListUseCase,
        )

        return operations.getCurrenciesStatusesFlow()
            .map { maybeCurrenciesStatuses ->
                maybeCurrenciesStatuses.mapLeft(CurrenciesStatusesOperations.Error::mapToTokenListError)
            }
    }

    private fun createTokenList(
        userWalletId: UserWalletId,
        tokens: List<CryptoCurrencyStatus>,
    ): Flow<Either<TokenListError, TokenList>> {
        val operations = TokenListOperations(
            userWalletId = userWalletId,
            tokens = tokens,
            useCase = this@GetTokenListUseCase,
        )

        return operations.getTokenListFlow().map { maybeTokenList ->
            maybeTokenList.mapLeft(TokenListOperations.Error::mapToTokenListError)
        }
    }
}