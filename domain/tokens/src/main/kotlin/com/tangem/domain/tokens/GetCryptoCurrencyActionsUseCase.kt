package com.tangem.domain.tokens

import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class GetCryptoCurrencyActionsUseCase(
    private val dispatchers: CoroutineDispatcherProvider,
) {

    operator fun invoke(userWalletId: UserWalletId, tokenId: String): Flow<TokenActionsState> {
        return flow {
            emit(getMockState(userWalletId, tokenId))
        }.flowOn(dispatchers.io)
    }

    // TODO replace by real data
    private fun getMockState(userWalletId: UserWalletId, tokenId: String): TokenActionsState {
        return TokenActionsState(
            walletId = userWalletId,
            tokenId = tokenId,
            states = listOf(
                TokenActionsState.ActionState.Buy(true),
                TokenActionsState.ActionState.Send(true),
                TokenActionsState.ActionState.Receive(true),
                TokenActionsState.ActionState.Sell(true),
                TokenActionsState.ActionState.Swap(true),
            ),
        )
    }
}