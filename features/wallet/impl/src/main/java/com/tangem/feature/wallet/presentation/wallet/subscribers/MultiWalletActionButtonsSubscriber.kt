package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.promo.GetStoryContentUseCase
import com.tangem.domain.promo.models.StoryContentIds
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.transformers.UpdateMultiWalletActionButtonBadgeTransformer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class MultiWalletActionButtonsSubscriber(
    private val userWallet: UserWallet,
    private val stateHolder: WalletStateController,
    private val getStoryContentUseCase: GetStoryContentUseCase,
) : WalletSubscriber() {
    override fun create(coroutineScope: CoroutineScope): Flow<*> = getStoryContentUseCase(
        id = StoryContentIds.STORY_FIRST_TIME_SWAP.id,
    ).map { maybeSwapStories ->
        val isSwapStoriesNotNull = maybeSwapStories.getOrNull() != null
        stateHolder.update(
            UpdateMultiWalletActionButtonBadgeTransformer(
                userWalletId = userWallet.walletId,
                showSwapBadge = isSwapStoriesNotNull,
            ),
        )
    }
}