package com.tangem.domain.tokens

import com.tangem.domain.tokens.model.ButtonState
import com.tangem.domain.tokens.model.WalletButtonsState
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*

class GetButtonsStateUseCase(
    internal val dispatchers: CoroutineDispatcherProvider,
) {

    operator fun invoke(
        userWalletId: UserWalletId,
    ): Flow<WalletButtonsState> {
        return flow {
            emit(getMockState(userWalletId))
        }.flowOn(dispatchers.io)
    }

    // TODO replace by real data
    private fun getMockState(userWalletId: UserWalletId) : WalletButtonsState {
        return WalletButtonsState(
            walletId = userWalletId,
            states =  listOf(
                ButtonState.Buy(true),
                ButtonState.Sell(true),
                ButtonState.Receive(true),
                ButtonState.Exchange(true)
            )
        )
    }


}
