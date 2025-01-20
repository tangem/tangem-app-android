package com.tangem.features.onboarding.v2.multiwallet.impl.child.chooseoption

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildParams
import com.tangem.features.onboarding.v2.multiwallet.impl.child.chooseoption.model.Wallet1ChooseOptionModel
import com.tangem.features.onboarding.v2.multiwallet.impl.child.chooseoption.ui.Wallet1ChooseOption
import com.tangem.features.onboarding.v2.multiwallet.impl.model.OnboardingMultiWalletState
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class Wallet1ChooseOptionComponent(
    context: AppComponentContext,
    params: MultiWalletChildParams,
    private val onNextStep: (OnboardingMultiWalletState.Step) -> Unit,
) : AppComponentContext by context, MultiWalletChildComponent {

    private val model: Wallet1ChooseOptionModel = getOrCreateModel(params)

    init {
        params.innerNavigation.update {
            it.copy(
                stackSize = 3,
                stackMaxSize = 8,
            )
        }

        params.parentParams.titleProvider.changeTitle(
            text = resourceReference(R.string.onboarding_getting_started),
        )

        componentScope.launch {
            model.returnToParentFlow.collect {
                onNextStep(OnboardingMultiWalletState.Step.Done)
            }
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        Wallet1ChooseOption(
            canSkipBackup = model.canSkipBackup,
            onBackupClick = {
                onNextStep(OnboardingMultiWalletState.Step.AddBackupDevice)
            },
            onSkipClick = remember(model) {
                {
                    model.onSkipClick()
                }
            },
        )
    }
}