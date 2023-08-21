package com.tangem.domain.tokens

import com.tangem.domain.tokens.model.WalletActionsState
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*

class GetWalletActionsUseCase(
    private val dispatchers: CoroutineDispatcherProvider,
) {

    operator fun invoke(
        userWalletId: UserWalletId,
    ): Flow<WalletActionsState> {
        return flow {
            emit(getMockState(userWalletId))
        }.flowOn(dispatchers.io)
    }

    // TODO replace by real data
    private fun getMockState(userWalletId: UserWalletId) : WalletActionsState {
        return WalletActionsState(
            walletId = userWalletId,
            states =  listOf(
                WalletActionsState.ActionState.Buy(true),
                WalletActionsState.ActionState.Sell(true),
                WalletActionsState.ActionState.Receive(true),
                WalletActionsState.ActionState.Exchange(true)
            )
        )
    }

}
