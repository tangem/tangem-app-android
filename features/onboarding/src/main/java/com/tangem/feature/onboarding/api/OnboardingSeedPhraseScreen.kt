package com.tangem.feature.onboarding.api

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.onboarding.presentation.wallet2.model.OnboardingSeedPhraseState
import com.tangem.feature.onboarding.presentation.wallet2.ui.*
import com.tangem.feature.onboarding.presentation.wallet2.viewmodel.SeedPhraseScreen

/**
* [REDACTED_AUTHOR]
 */
class OnboardingSeedPhraseScreen : OnboardingSeedPhraseApi {

    @Composable
    override fun ScreenContent(uiState: OnboardingSeedPhraseState, subScreen: SeedPhraseScreen, progress: Float) {
        BackHandler(onBack = uiState.onBackClick)
        Column {
            ProgressIndicator(progress)
            Content(subScreen, uiState)
        }
    }
}

@Composable
private fun ProgressIndicator(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(top = TangemTheme.dimens.size8),
    ) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TangemTheme.dimens.size16),
            color = TangemTheme.colors.background.action,
            backgroundColor = TangemTheme.colors.background.action.copy(alpha = 0.081f),
            progress = progress,
        )
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
