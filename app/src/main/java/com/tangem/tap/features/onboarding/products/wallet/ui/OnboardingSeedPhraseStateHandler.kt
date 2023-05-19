package com.tangem.tap.features.onboarding.products.wallet.ui

import androidx.compose.runtime.collectAsState
import com.tangem.feature.onboarding.api.OnboardingSeedPhrase
import com.tangem.feature.onboarding.api.OnboardingSeedPhraseApi
import com.tangem.feature.onboarding.presentation.wallet2.viewmodel.SeedPhraseViewModel
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.tap.features.onboarding.products.wallet.redux.OnboardingWalletState
import com.tangem.tap.features.onboarding.products.wallet.redux.OnboardingWalletStep

/**
[REDACTED_AUTHOR]
 */
internal class OnboardingSeedPhraseStateHandler(
    private val onboardingSeedPhraseApi: OnboardingSeedPhraseApi = OnboardingSeedPhrase(),
) {

    fun newState(
        walletFragment: OnboardingWalletFragment,
        state: OnboardingWalletState,
        seedPhraseViewModel: SeedPhraseViewModel,
    ) {
        if (state.step == OnboardingWalletStep.CreateWallet && !seedPhraseViewModel.isFinished) {
            switchToWallet2SeedPhraseOnboarding(walletFragment, seedPhraseViewModel, state.getMaxProgress())
        } else {
            switchToWalletOnboarding(walletFragment, state)
        }
    }

    private fun switchToWalletOnboarding(walletFragment: OnboardingWalletFragment, state: OnboardingWalletState) {
        walletFragment.bindingSeedPhrase.onboardingSeedPhraseContainer.hide()
        walletFragment.pbBinding.pbState.show()
        walletFragment.binding.onboardingWalletContainer.show()
        walletFragment.handleOnboardingStep(state)
    }

    private fun switchToWallet2SeedPhraseOnboarding(
        walletFragment: OnboardingWalletFragment,
        viewModel: SeedPhraseViewModel,
        onboardingWalletMaxProgress: Int,
    ) {
        walletFragment.pbBinding.pbState.hide()
        walletFragment.binding.onboardingWalletContainer.hide()
        walletFragment.bindingSeedPhrase.onboardingSeedPhraseContainer.show()
        walletFragment.bindingSeedPhrase.onboardingSeedPhraseContainer.setContent {
            onboardingSeedPhraseApi.ScreenContent(
                uiState = viewModel.uiState,
                subScreen = viewModel.currentScreen.collectAsState().value,
                progress = viewModel.progress.collectAsState(0).value.toFloat() / onboardingWalletMaxProgress,
            )
        }
    }
}