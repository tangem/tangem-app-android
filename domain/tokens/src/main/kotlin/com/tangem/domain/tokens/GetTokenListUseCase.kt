package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.Raise
import arrow.core.raise.recover
import arrow.core.right
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.error.mapper.mapToTokenListError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.tokens.operations.CurrenciesStatusesOperations
import com.tangem.domain.tokens.operations.TokenListOperations
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.tokens.repository.TokensRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapConcat

class GetTokenListUseCase(
    internal val tokensRepository: TokensRepository,
    internal val quotesRepository: QuotesRepository,
    internal val networksRepository: NetworksRepository,
    internal val dispatchers: CoroutineDispatcherProvider,
) {

    operator fun invoke(userWalletId: UserWalletId, refresh: Boolean = true): Flow<Either<TokenListError, TokenList>> {
        return channelFlow {
            recover(
                block = {
                    getTokenList(userWalletId, refresh).collectLatest { list ->
                        send(list.right())
                    }
                },
                recover = { error ->
                    send(error.left())
                },
            )
        }
    }
    private fun Raise<TokenListError>.getTokenList(userWalletId: UserWalletId, refresh: Boolean): Flow<TokenList> {
        return getTokensStatuses(userWalletId, refresh).flatMapConcat { tokens ->
            createTokenList(userWalletId, tokens)
        }
    }

    private fun Raise<TokenListError>.getTokensStatuses(
        userWalletId: UserWalletId,
        refresh: Boolean,
    ): Flow<Set<CryptoCurrencyStatus>> {
        val operations = CurrenciesStatusesOperations(
            userWalletId = userWalletId,
            refresh = refresh,
            useCase = this@GetTokenListUseCase,
            raise = this,
            transformError = CurrenciesStatusesOperations.Error::mapToTokenListError,
        )

        return operations.getMultiCurrencyWalletStatusesFlow()
    }

    private fun Raise<TokenListError>.createTokenList(
        userWalletId: UserWalletId,
        tokens: Set<CryptoCurrencyStatus>,
    ): Flow<TokenList> {
        val operations = TokenListOperations(
            userWalletId = userWalletId,
            tokens = tokens,
            useCase = this@GetTokenListUseCase,
            raise = this,
            transform = TokenListOperations.Error::mapToTokenListError,
        )

        return operations.getTokenListFlow()
    }
}