package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.Raise
import arrow.core.raise.recover
import arrow.core.right
import com.tangem.domain.tokens.error.TokenError
import com.tangem.domain.tokens.error.mapper.mapToTokenError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.operations.CurrenciesStatusesOperations
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.tokens.repository.TokensRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest

class GetPrimaryCurrencyUseCase(
    private val tokensRepository: TokensRepository,
    private val quotesRepository: QuotesRepository,
    private val networksRepository: NetworksRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    operator fun invoke(
        userWalletId: UserWalletId,
        refresh: Boolean = false,
    ): Flow<Either<TokenError, CryptoCurrencyStatus>> {
        return channelFlow {
            recover(
                block = {
                    getToken(userWalletId, refresh).collectLatest { token ->
                        send(token.right())
                    }
                },
                recover = { error ->
                    send(error.left())
                },
            )
        }
    }

    private suspend fun Raise<TokenError>.getToken(
        userWalletId: UserWalletId,
        refresh: Boolean,
    ): Flow<CryptoCurrencyStatus> {
        val operations = CurrenciesStatusesOperations(
            tokensRepository = tokensRepository,
            quotesRepository = quotesRepository,
            networksRepository = networksRepository,
            userWalletId = userWalletId,
            refresh = refresh,
            dispatchers = dispatchers,
            raise = this,
            transformError = CurrenciesStatusesOperations.Error::mapToTokenError,
        )

        return operations.getPrimaryCurrencyStatusFlow()
    }
}
