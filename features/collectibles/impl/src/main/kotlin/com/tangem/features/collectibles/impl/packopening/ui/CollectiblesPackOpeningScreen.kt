package com.tangem.features.collectibles.impl.packopening.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.features.collectibles.impl.packopening.ui.components.confirmorder.ConfirmOrder
import com.tangem.features.collectibles.impl.packopening.ui.components.confirmorder.rememberPackLaunchProgress
import com.tangem.features.collectibles.impl.packopening.ui.components.packreveal.PackReveal
import com.tangem.features.collectibles.impl.packopening.ui.components.rewardcard.RewardCard
import com.tangem.features.collectibles.impl.packopening.ui.components.rewardcard.RewardCardTiming
import com.tangem.features.collectibles.impl.packopening.ui.components.rewardcard.rememberCardAspect
import com.tangem.features.collectibles.impl.packopening.ui.state.CollectiblesPackOpeningUM
import com.tangem.features.collectibles.impl.packopening.ui.state.CollectiblesPackOpeningUM.Stage

@Composable
internal fun CollectiblesPackOpeningScreen(state: CollectiblesPackOpeningUM, modifier: Modifier = Modifier) {
    val content = state as? CollectiblesPackOpeningUM.Content
    val reveal = content?.reveal
    val stage = reveal?.stage ?: Stage.Order

    val card = reveal?.card
    val cardAspect = rememberCardAspect(frontImageUrl = card?.frontImageUrl)

    val launchProgress = rememberPackLaunchProgress(
        isLaunching = stage == Stage.Launching,
        onFinish = { reveal?.onPackAnimationFinished?.invoke() },
    )

    Box(modifier = modifier.fillMaxSize()) {
        if (reveal != null) {
            PackReveal(
                onSurfaceAttach = reveal.onSurfaceAttached,
                onSurfaceDetach = reveal.onSurfaceDetached,
                isTapEnabled = stage == Stage.Reveal,
                onTap = reveal.onRevealTap,
            )
        }

        if (content != null && card != null && stage == Stage.Reward) {
            RewardCard(
                um = card,
                timing = if (content.reveal.isSkipped) RewardCardTiming.Skipped else RewardCardTiming.Default,
                cardAspect = cardAspect,
                onKeepCardClick = content.reveal.onKeepCardClick,
                onSellBackClick = content.reveal.onSellBackClick,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (stage == Stage.Order || stage == Stage.Launching) {
            ConfirmOrder(state = state, progress = launchProgress)
        }
    }
}