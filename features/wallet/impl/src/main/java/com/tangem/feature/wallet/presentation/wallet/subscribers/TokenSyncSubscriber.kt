package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tokensync.model.TokenSyncProgress
import com.tangem.domain.tokensync.usecase.ObserveTokenSyncUseCase
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.model.TokenSyncProgressUM
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetTokenSyncProgressTransformer
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

internal class TokenSyncSubscriber @AssistedInject constructor(
    @Assisted private val userWallet: UserWallet,
    private val stateController: WalletStateController,
    private val observeTokenSyncUseCase: ObserveTokenSyncUseCase,
) : WalletSubscriber() {

    override fun create(coroutineScope: CoroutineScope): Flow<*> {
        return observeTokenSyncUseCase(userWallet.walletId)
            .onEach { current -> handleProgress(userWallet, current) }
    }

    private fun handleProgress(userWallet: UserWallet, current: TokenSyncProgress) {
        val progressUM = when (current) {
            is TokenSyncProgress.InProgress -> TokenSyncProgressUM.InProgress(current.progressPercent)
            is TokenSyncProgress.Completed -> TokenSyncProgressUM.Completed
            is TokenSyncProgress.Idle -> TokenSyncProgressUM.Idle
        }
        stateController.update(
            SetTokenSyncProgressTransformer(
                userWallet = userWallet,
                progress = progressUM,
            ),
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(userWallet: UserWallet): TokenSyncSubscriber
    }
}