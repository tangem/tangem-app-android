package com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.res.TangemAnimations
import com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.ui.state.MultiWalletSeedPhraseUM

@Composable
internal fun MultiWalletSeedPhrase(state: MultiWalletSeedPhraseUM, modifier: Modifier = Modifier) {
    AnimatedContent(
        modifier = modifier.navigationBarsPadding(),
        targetState = state,
        transitionSpec = TangemAnimations.AnimatedContent.slide { from, to -> to.order > from.order },
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