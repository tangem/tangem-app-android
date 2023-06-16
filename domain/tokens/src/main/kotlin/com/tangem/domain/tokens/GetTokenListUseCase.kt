// TODO: Will be used in https://tangem.atlassian.net/browse/AND-3814
@file:Suppress("UNUSED", "UNUSED_PARAMETER")

package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.left
import com.tangem.domain.models.userwallet.UserWalletId
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class GetTokenListUseCase(
    private val dispatchers: CoroutineDispatcherProvider,
    private val networksRepository: NetworksRepository,
) {

    operator fun invoke(userWalletId: UserWalletId): Flow<Either<TokensError, TokenList>> {
        return flowOf(EmptyTokens.left())
    }
}
