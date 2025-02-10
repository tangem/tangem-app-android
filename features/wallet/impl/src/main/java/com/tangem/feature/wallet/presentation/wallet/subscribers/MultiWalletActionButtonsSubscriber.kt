package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.promo.ShouldShowSwapStoriesUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.transformers.UpdateMultiWalletActionButtonBadgeTransformer
import com.tangem.features.swap.SwapFeatureToggles
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class MultiWalletActionButtonsSubscriber(
    private val userWallet: UserWallet,
    private val stateHolder: WalletStateController,
    private val shouldShowSwapStoriesUseCase: ShouldShowSwapStoriesUseCase,
    private val swapFeatureToggles: SwapFeatureToggles,
) : WalletSubscriber() {
    override fun create(coroutineScope: CoroutineScope): Flow<*> = shouldShowSwapStoriesUseCase()
        .distinctUntilChanged()
        .map { showSwapBadge ->
            val showBadge = swapFeatureToggles.isPromoStoriesEnabled && showSwapBadge
            stateHolder.update(
                UpdateMultiWalletActionButtonBadgeTransformer(
                    userWalletId = userWallet.walletId,
                    showSwapBadge = showBadge,
                ),
            )
        }
}