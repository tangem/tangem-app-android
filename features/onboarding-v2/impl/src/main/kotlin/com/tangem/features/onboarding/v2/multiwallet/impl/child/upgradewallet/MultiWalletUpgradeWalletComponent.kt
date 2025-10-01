package com.tangem.features.onboarding.v2.multiwallet.impl.child.upgradewallet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.essenty.lifecycle.doOnStart
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildParams
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.child.upgradewallet.model.MultiWalletUpgradeWalletModel
import com.tangem.features.onboarding.v2.multiwallet.impl.child.upgradewallet.ui.MultiWalletUpgradeWallet
import com.tangem.features.onboarding.v2.multiwallet.impl.model.OnboardingMultiWalletState
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class MultiWalletUpgradeWalletComponent(
    context: AppComponentContext,
    params: MultiWalletChildParams,
    onNextStep: (OnboardingMultiWalletState.Step) -> Unit,
) : AppComponentContext by context, MultiWalletChildComponent {

    private val model: MultiWalletUpgradeWalletModel = getOrCreateModel(params)

    init {
        lifecycle.doOnStart {
            params.innerNavigation.update {
                it.copy(
                    stackSize = 2,
                    stackMaxSize = 9,
                )
            }

            params.parentParams.titleProvider.changeTitle(
                text = resourceReference(R.string.common_tangem),
            )
        }

        componentScope.launch {
            model.onDone.collect(onNextStep)
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        MultiWalletUpgradeWallet(
            modifier = modifier,
            state = state,
        )
    }
}