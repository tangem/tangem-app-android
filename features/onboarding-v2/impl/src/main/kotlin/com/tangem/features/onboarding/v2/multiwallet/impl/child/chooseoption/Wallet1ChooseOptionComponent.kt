package com.tangem.features.onboarding.v2.multiwallet.impl.child.chooseoption

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.child.chooseoption.ui.Wallet1ChooseOption
import com.tangem.features.onboarding.v2.multiwallet.impl.model.OnboardingMultiWalletState

class Wallet1ChooseOptionComponent(
    context: AppComponentContext,
    private val onNextStep: (OnboardingMultiWalletState.Step) -> Unit,
) : AppComponentContext by context, MultiWalletChildComponent {

    @Composable
    override fun Content(modifier: Modifier) {
        Wallet1ChooseOption(
            onBackupClick = {
                onNextStep(OnboardingMultiWalletState.Step.AddBackupDevice)
            },
            onScipClick = {
                onNextStep(OnboardingMultiWalletState.Step.Done)
            },
        )
    }
}
