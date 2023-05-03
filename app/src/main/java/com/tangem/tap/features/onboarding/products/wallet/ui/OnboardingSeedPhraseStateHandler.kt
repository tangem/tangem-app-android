package com.tangem.tap.features.onboarding.products.wallet.ui

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
    private val walletFragment: OnboardingWalletFragment,
    private val onboardingSeedPhraseApi: OnboardingSeedPhraseApi = OnboardingSeedPhrase(),
) {

    fun newState(state: OnboardingWalletState, seedPhraseViewModel: SeedPhraseViewModel) {
        if (state.step == OnboardingWalletStep.CreateWallet && !seedPhraseViewModel.isFinished) {
            switchToWallet2SeedPhraseOnboarding(state, seedPhraseViewModel)
        } else {
            switchToWalletOnboarding(state)
        }
    }

    private fun switchToWalletOnboarding(state: OnboardingWalletState) {
        walletFragment.bindingSeedPhrase.onboardingSeedPhraseContainer.hide()
        walletFragment.pbBinding.pbState.show()
        walletFragment.binding.onboardingWalletContainer.show()
        walletFragment.handleOnboardingStep(state)
    }

    private fun switchToWallet2SeedPhraseOnboarding(
        state: OnboardingWalletState,
        seedPhraseViewModel: SeedPhraseViewModel,
    ) {
        walletFragment.pbBinding.pbState.hide()
        walletFragment.binding.onboardingWalletContainer.hide()
        walletFragment.bindingSeedPhrase.onboardingSeedPhraseContainer.show()
        walletFragment.bindingSeedPhrase.onboardingSeedPhraseContainer.setContent {
            onboardingSeedPhraseApi.ScreenContent(
                viewModel = seedPhraseViewModel,
                maxProgress = state.getMaxProgress(),
            )
        }
    }
}