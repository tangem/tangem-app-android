package com.tangem.features.onboarding.v2.multiwallet.impl.child.chooseoption

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildParams
import com.tangem.features.onboarding.v2.multiwallet.impl.child.chooseoption.ui.Wallet1ChooseOption
import com.tangem.features.onboarding.v2.multiwallet.impl.model.OnboardingMultiWalletState
import kotlinx.coroutines.flow.update

class Wallet1ChooseOptionComponent(
    context: AppComponentContext,
    params: MultiWalletChildParams,
    private val onNextStep: (OnboardingMultiWalletState.Step) -> Unit,
) : AppComponentContext by context, MultiWalletChildComponent {

    init {
        params.innerNavigation.update {
            it.copy(
                stackSize = 3,
                stackMaxSize = 8,
            )
        }

        params.parentParams.titleProvider.changeTitle(
            text = resourceReference(R.string.onboarding_create_wallet_header),
        )
    }

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