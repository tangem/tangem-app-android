package com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.ui.state.MultiWalletSeedPhraseUM

@Composable
fun MultiWalletSeedPhrase(state: MultiWalletSeedPhraseUM, modifier: Modifier = Modifier) {
    AnimatedContent(
        modifier = modifier.navigationBarsPadding(),
        targetState = state,
        transitionSpec = {
            val direction = if (initialState.order < targetState.order) {
                AnimatedContentTransitionScope.SlideDirection.Start
            } else {
                AnimatedContentTransitionScope.SlideDirection.End
            }

            slideIntoContainer(towards = direction, animationSpec = tween())
                .togetherWith(slideOutOfContainer(towards = direction, animationSpec = tween()))
        },
        contentKey = { st -> st::class.java },
        label = "animatedContent",
    ) { st ->
        when (st) {
            is MultiWalletSeedPhraseUM.Start -> MultiWalletSeedPhraseStart(state = st)
            is MultiWalletSeedPhraseUM.GenerateSeedPhrase -> MultiWalletSeedPhraseWords(state = st)
            is MultiWalletSeedPhraseUM.GeneratedWordsCheck -> MultiWalletSeedPhraseWordsCheck(state = st)
            is MultiWalletSeedPhraseUM.Import -> MultiWalletSeedPhraseImport(state = st)
        }
    }
}