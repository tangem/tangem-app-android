package com.tangem.feature.onboarding.api

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import com.tangem.core.ui.components.progressbar.TangemLinearProgressIndicator
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.onboarding.presentation.wallet2.model.OnboardingSeedPhraseState
import com.tangem.feature.onboarding.presentation.wallet2.ui.*
import com.tangem.feature.onboarding.presentation.wallet2.viewmodel.SeedPhraseScreen

/**
[REDACTED_AUTHOR]
 */
class OnboardingSeedPhraseScreen : OnboardingSeedPhraseApi {

    @Composable
    override fun ScreenContent(uiState: OnboardingSeedPhraseState, subScreen: SeedPhraseScreen, progress: Float) {
        BackHandler(onBack = uiState.onBackClick)
        Column {
            TangemLinearProgressIndicator(
                progress = { progress },
                color = TangemTheme.colors.icon.primary1,
                backgroundColor = TangemTheme.colors.icon.primary1.copy(alpha = 0.4f),
                strokeCap = StrokeCap.Round,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(top = TangemTheme.dimens.size8)
                    .padding(horizontal = TangemTheme.dimens.size16),
            )
            Content(subScreen, uiState)
        }
    }
}

@Composable
private fun Content(screen: SeedPhraseScreen, uiState: OnboardingSeedPhraseState) {
    when (screen) {
        SeedPhraseScreen.Intro -> {
            IntroScreen(
                state = uiState.introState,
            )
        }
        SeedPhraseScreen.AboutSeedPhrase -> {
            AboutSeedPhraseScreen(
                state = uiState.aboutState,
            )
        }
        SeedPhraseScreen.YourSeedPhrase -> {
            YourSeedPhraseScreen(
                state = uiState.yourSeedPhraseState,
            )
        }
        SeedPhraseScreen.CheckSeedPhrase -> {
            CheckSeedPhraseScreen(
                state = uiState.checkSeedPhraseState,
            )
        }
        SeedPhraseScreen.ImportSeedPhrase -> {
            ImportSeedPhraseScreen(
                state = uiState.importSeedPhraseState,
            )
        }
    }
}