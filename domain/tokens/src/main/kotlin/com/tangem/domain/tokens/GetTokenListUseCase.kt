package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.Raise
import arrow.core.raise.recover
import arrow.core.right
import com.tangem.domain.tokens.error.TokensError
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.tokens.model.TokenStatus
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.tokens.repository.TokensRepository
import com.tangem.domain.tokens.utils.TokenListFiatBalanceOperations
import com.tangem.domain.tokens.utils.TokenListOperations
import com.tangem.domain.tokens.utils.TokensStatusesOperations
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flatMapConcat

class GetTokenListUseCase(
    private val tokensRepository: TokensRepository,
    private val quotesRepository: QuotesRepository,
    private val networksRepository: NetworksRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    operator fun invoke(userWalletId: UserWalletId, refresh: Boolean = true): Flow<Either<TokensError, TokenList>> {
        return channelFlow {
            recover(
                block = {
                    getTokenList(userWalletId, refresh).collect { list ->
                        send(list.right())
                    }
                },
                recover = { error ->
                    send(error.left())
                },
            )
        }
    }
    private fun Raise<TokensError>.getTokenList(userWalletId: UserWalletId, refresh: Boolean): Flow<TokenList> {
        return getTokensStatuses(userWalletId, refresh).flatMapConcat { tokens ->
            createTokenList(userWalletId, tokens)
        }
    }

    private fun Raise<TokensError>.getTokensStatuses(
        userWalletId: UserWalletId,
        refresh: Boolean,
    ): Flow<Set<TokenStatus>> {
        val operations = TokensStatusesOperations(
            tokensRepository,
            quotesRepository,
            networksRepository,
            userWalletId,
            refresh,
            dispatchers,
            raise = this,
        )

        return operations.getTokensStatusesFlow()
    }

    private suspend fun Raise<TokensError>.createTokenList(
        userWalletId: UserWalletId,
        tokens: Set<TokenStatus>,
    ): Flow<TokenList> {
        val isAnyTokenLoading = tokens.any { it.value is TokenStatus.Loading }
        val operations = TokenListOperations(
            tokensRepository,
            networksRepository,
            userWalletId,
            calculateFiatBalance(tokens, isAnyTokenLoading),
            isAnyTokenLoading,
            dispatchers,
            raise = this,
        )

        return operations.getTokenListFlow(tokens)
    }

    private suspend fun calculateFiatBalance(
        tokens: Set<TokenStatus>,
        isAnyTokenLoading: Boolean,
    ): TokenList.FiatBalance {
        val operations = TokenListFiatBalanceOperations(tokens, isAnyTokenLoading, dispatchers)

        return operations.calculateFiatBalance()
    }
}
