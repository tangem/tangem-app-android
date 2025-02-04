package com.tangem.features.onboarding.v2.visa.impl.child.choosewallet

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.onboarding.v2.visa.impl.DefaultOnboardingVisaComponent
import com.tangem.features.onboarding.v2.visa.impl.child.choosewallet.model.OnboardingVisaChooseWalletModel
import com.tangem.features.onboarding.v2.visa.impl.child.choosewallet.ui.OnboardingVisaChooseWallet
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class OnboardingVisaChooseWalletComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: OnboardingVisaChooseWalletModel = getOrCreateModel()

    init {
        model.onEvent
            .onEach { params.onEvent(it) }
            .launchIn(componentScope)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        BackHandler(onBack = remember(this) { { params.childParams.onBack() } })

        OnboardingVisaChooseWallet(
            modifier = modifier,
            state = state,
        )
    }

    data class Params(
        val childParams: DefaultOnboardingVisaComponent.ChildParams,
        val onEvent: (Event) -> Unit,
    ) {
        enum class Event {
            TangemWallet, OtherWallet
        }
    }
}