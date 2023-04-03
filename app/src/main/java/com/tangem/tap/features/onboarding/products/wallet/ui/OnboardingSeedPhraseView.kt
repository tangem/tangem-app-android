package com.tangem.tap.features.onboarding.products.wallet.ui

import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import com.google.android.material.appbar.MaterialToolbar
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.onboarding.presentation.wallet2.model.OnboardingSeedPhraseStep
import com.tangem.feature.onboarding.presentation.wallet2.ui.AboutSeedPhraseScreen
import com.tangem.feature.onboarding.presentation.wallet2.ui.CheckSeedPhraseScreen
import com.tangem.feature.onboarding.presentation.wallet2.ui.IntroScreen
import com.tangem.feature.onboarding.presentation.wallet2.ui.YourSeedPhraseScreen
import com.tangem.feature.onboarding.presentation.wallet2.viewmodel.SeedPhraseViewModel
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.tap.features.onboarding.products.wallet.redux.OnboardingWalletState
import com.tangem.tap.features.onboarding.products.wallet.redux.OnboardingWalletStep

/**
 * Created by Anton Zhilenkov on 20.10.2022.
 */
internal class OnboardingSeedPhraseView(
    private val walletFragment: OnboardingWalletFragment,
) {

    private val toolbar: MaterialToolbar by lazy { walletFragment.binding.toolbar }

    fun newState(state: OnboardingWalletState, viewModel: SeedPhraseViewModel) {
        when (state.step) {
            OnboardingWalletStep.SeedPhrase -> setSeedPhraseStep(viewModel)
            else -> walletFragment.handleOnboardingStep(state)
        }
    }

    private fun setSeedPhraseStep(viewModel: SeedPhraseViewModel) {
        walletFragment.binding.onboardingWalletContainer.hide()
        walletFragment.bindingSaltPay.onboardingSaltpayContainer.hide()

        val container = walletFragment.bindingSeedPhrase.onboardingSeedPhraseContainer
        container.show()
        if (container.childCount == 0) {
            val composerView = ComposeView(container.context)
            container.addView(composerView)

            composerView.setContent {
                TangemTheme {
                    ScreenContent(viewModel)
                }
            }
        }
    }

    @Composable
    private fun ScreenContent(viewModel: SeedPhraseViewModel) {
        when (viewModel.currentStep) {
            OnboardingSeedPhraseStep.Intro -> {
                IntroScreen(state = viewModel.uiState.introState)
            }
            OnboardingSeedPhraseStep.AboutSeedPhrase -> {
                AboutSeedPhraseScreen(state = viewModel.uiState.aboutState)
            }
            OnboardingSeedPhraseStep.YourSeedPhrase -> {
                YourSeedPhraseScreen(state = viewModel.uiState.yourSeedPhraseState)
            }
            OnboardingSeedPhraseStep.CheckSeedPhrase -> {
                CheckSeedPhraseScreen(
                    modifier = Modifier.imePadding(),
                    state = viewModel.uiState.checkSeedPhraseState,
                )
            }
            OnboardingSeedPhraseStep.ImportSeedPhrase -> {
                // TODO: implement
            }
        }
    }
}
