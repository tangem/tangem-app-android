package com.tangem.tap.features.develop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.onboarding.presentation.wallet2.model.OnboardingSeedPhraseStep
import com.tangem.feature.onboarding.presentation.wallet2.ui.AboutSeedPhraseScreen
import com.tangem.feature.onboarding.presentation.wallet2.ui.CheckSeedPhraseScreen
import com.tangem.feature.onboarding.presentation.wallet2.ui.ImportSeedPhraseScreen
import com.tangem.feature.onboarding.presentation.wallet2.ui.IntroScreen
import com.tangem.feature.onboarding.presentation.wallet2.ui.YourSeedPhraseScreen
import com.tangem.feature.onboarding.presentation.wallet2.viewmodel.SeedPhraseViewModel
import com.tangem.tap.features.details.ui.common.EmptyTopBarWithNavigation
import dagger.hilt.android.AndroidEntryPoint

/**
 * Created by Anton Zhilenkov on 11.03.2023.
 * Temporary fragment to test Wallet2 onboarding screens
 */
@AndroidEntryPoint
class DevelopFragment : Fragment() {

    private val viewModel by viewModels<SeedPhraseViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(inflater.context).apply {
            setContent {
                ScreenContent()
            }
        }
    }

    @Composable
    private fun ScreenContent() {
        TangemTheme {
            Scaffold(
                topBar = {
                    EmptyTopBarWithNavigation(
                        onBackClick = { },
                    )
                },
                modifier = Modifier
                    .systemBarsPadding()
                    .background(color = TangemTheme.colors.background.secondary),
                content = { padding ->
                    Column {
                        Box(
                            modifier = Modifier
                                .background(Color.Cyan)
                                .fillMaxWidth()
                                .height(TangemTheme.dimens.size32),
                        ) {
                            val maxProgress = OnboardingSeedPhraseStep.values().size
                            val progress = (viewModel.currentStep.ordinal / maxProgress).toFloat()
                            LinearProgressIndicator(progress = progress)
                        }

                        val modifier = Modifier.padding(padding)
                        when (viewModel.currentStep) {
                            OnboardingSeedPhraseStep.Intro -> {
                                IntroScreen(
                                    modifier = modifier,
                                    state = viewModel.uiState.introState,
                                )
                            }
                            OnboardingSeedPhraseStep.AboutSeedPhrase -> {
                                AboutSeedPhraseScreen(
                                    modifier = modifier,
                                    state = viewModel.uiState.aboutState,
                                )
                            }
                            OnboardingSeedPhraseStep.YourSeedPhrase -> {
                                YourSeedPhraseScreen(
                                    modifier = modifier,
                                    state = viewModel.uiState.yourSeedPhraseState,
                                )
                            }
                            OnboardingSeedPhraseStep.CheckSeedPhrase -> {
                                CheckSeedPhraseScreen(
                                    modifier = modifier,
                                    state = viewModel.uiState.checkSeedPhraseState,
                                )
                            }
                            OnboardingSeedPhraseStep.ImportSeedPhrase -> {
                                ImportSeedPhraseScreen(
                                    modifier = modifier,
                                    state = viewModel.uiState.importSeedPhraseState,
                                )
                            }
                        }
                    }
                },
            )
        }
    }
}
