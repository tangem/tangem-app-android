// [REDACTED_TODO_COMMENT]
@file:Suppress("unused", "unused_parameter")

package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.left
import com.tangem.domain.tokens.error.TokensError
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.tokens.repository.TokensRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class GetTokenListUseCase(
    private val tokensRepository: TokensRepository,
    private val quotesRepository: QuotesRepository,
    private val networksRepository: NetworksRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    operator fun invoke(userWalletId: UserWalletId, refresh: Boolean = true): Flow<Either<TokensError, TokenList>> {
        return flowOf(TokensError.EmptyTokens.left())
    }
}
