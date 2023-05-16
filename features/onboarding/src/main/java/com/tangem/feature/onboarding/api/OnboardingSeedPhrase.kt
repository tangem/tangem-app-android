package com.tangem.feature.onboarding.api

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.onboarding.presentation.wallet2.ui.AboutSeedPhraseScreen
import com.tangem.feature.onboarding.presentation.wallet2.ui.CheckSeedPhraseScreen
import com.tangem.feature.onboarding.presentation.wallet2.ui.ImportSeedPhraseScreen
import com.tangem.feature.onboarding.presentation.wallet2.ui.IntroScreen
import com.tangem.feature.onboarding.presentation.wallet2.ui.YourSeedPhraseScreen
import com.tangem.feature.onboarding.presentation.wallet2.viewmodel.SeedPhraseScreen
import com.tangem.feature.onboarding.presentation.wallet2.viewmodel.SeedPhraseViewModel

/**
[REDACTED_AUTHOR]
 */
class OnboardingSeedPhrase : OnboardingSeedPhraseApi {

    @Composable
    override fun ScreenContent(viewModel: SeedPhraseViewModel, maxProgress: Int) {
        BackHandler(onBack = viewModel.uiState.onBackClick)
        TangemTheme {
            Column {
                ProgressIndicator(viewModel.progress.collectAsState(0).value, maxProgress)
                Content(viewModel)
            }
        }
    }
}

@Composable
private fun ProgressIndicator(progress: Int, maxProgress: Int) {
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
            progress = progress.toFloat() / maxProgress,
        )
    }
}

@Composable
private fun Content(viewModel: SeedPhraseViewModel) {
    when (viewModel.currentScreen.collectAsState().value) {
        SeedPhraseScreen.Intro -> {
            IntroScreen(
                state = viewModel.uiState.introState,
            )
        }

        SeedPhraseScreen.AboutSeedPhrase -> {
            AboutSeedPhraseScreen(
                state = viewModel.uiState.aboutState,
            )
        }
        SeedPhraseScreen.YourSeedPhrase -> {
            YourSeedPhraseScreen(
                state = viewModel.uiState.yourSeedPhraseState,
            )
        }
        SeedPhraseScreen.CheckSeedPhrase -> {
            CheckSeedPhraseScreen(
                state = viewModel.uiState.checkSeedPhraseState,
            )
        }
        SeedPhraseScreen.ImportSeedPhrase -> {
            ImportSeedPhraseScreen(
                state = viewModel.uiState.importSeedPhraseState,
            )
        }
    }
}