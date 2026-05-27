package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.assetsdiscovery.model.AssetsDiscoveryProgress
import com.tangem.domain.assetsdiscovery.usecase.ObserveAssetsDiscoveryUseCase
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.model.AssetsDiscoveryProgressUM
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetAssetsDiscoveryProgressTransformer
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

internal class AssetsDiscoverySubscriber @AssistedInject constructor(
    @Assisted private val userWallet: UserWallet,
    private val stateController: WalletStateController,
    private val observeAssetsDiscoveryUseCase: ObserveAssetsDiscoveryUseCase,
) : WalletSubscriber() {

    override fun create(coroutineScope: CoroutineScope): Flow<*> {
        return observeAssetsDiscoveryUseCase(userWallet.walletId)
            .onEach { current -> handleProgress(userWallet, current) }
    }

    private fun handleProgress(userWallet: UserWallet, current: AssetsDiscoveryProgress) {
        val progressUM = when (current) {
            is AssetsDiscoveryProgress.InProgress -> AssetsDiscoveryProgressUM.InProgress(current.progressPercent)
            is AssetsDiscoveryProgress.Completed -> AssetsDiscoveryProgressUM.Completed
            is AssetsDiscoveryProgress.Idle -> AssetsDiscoveryProgressUM.Idle
        }
        stateController.update(
            SetAssetsDiscoveryProgressTransformer(
                userWallet = userWallet,
                progress = progressUM,
            ),
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(userWallet: UserWallet): AssetsDiscoverySubscriber
    }
}