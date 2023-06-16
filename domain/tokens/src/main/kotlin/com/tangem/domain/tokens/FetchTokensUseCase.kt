// TODO: Will be used in https://tangem.atlassian.net/browse/AND-3814
@file:Suppress("UNUSED", "UNUSED_PARAMETER")

package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.left
import com.tangem.domain.models.userwallet.UserWalletId
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.OrganizeTokensRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.tokens.repository.TokensRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider

class FetchTokensUseCase(
    private val dispatchers: CoroutineDispatcherProvider,
    private val tokensRepository: TokensRepository,
    private val quotesRepository: QuotesRepository,
    private val networksRepository: NetworksRepository,
    private val organizeTokensRepository: OrganizeTokensRepository,
) {

    operator fun invoke(userWalletId: UserWalletId, refresh: Boolean = false): Either<TokensError, Unit> {
        return EmptyTokens.left()
    }
}
