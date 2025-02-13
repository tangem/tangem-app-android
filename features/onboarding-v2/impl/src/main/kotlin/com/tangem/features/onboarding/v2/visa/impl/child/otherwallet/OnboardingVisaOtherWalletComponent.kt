package com.tangem.features.onboarding.v2.visa.impl.child.otherwallet

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.visa.model.VisaActivationOrderInfo
import com.tangem.features.onboarding.v2.visa.impl.DefaultOnboardingVisaComponent
import com.tangem.features.onboarding.v2.visa.impl.child.otherwallet.model.OnboardingVisaOtherWalletModel
import com.tangem.features.onboarding.v2.visa.impl.child.otherwallet.ui.OnboardingVisaOtherWallet
import com.tangem.features.onboarding.v2.visa.impl.common.PreparationDataForApprove
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class OnboardingVisaOtherWalletComponent(
    appComponentContext: AppComponentContext,
    config: Config,
    private val params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: OnboardingVisaOtherWalletModel = getOrCreateModel(config)

    init {
        model.onDone
            .onEach { params.onDone(it) }
            .launchIn(componentScope)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        BackHandler(onBack = remember(this) { { params.childParams.onBack() } })

        OnboardingVisaOtherWallet(
            modifier = modifier,
            state = state,
        )
    }

    data class Config(
        val scanResponse: ScanResponse,
        val preparationDataForApprove: PreparationDataForApprove,
    )

    data class Params(
        val childParams: DefaultOnboardingVisaComponent.ChildParams,
        val onDone: (VisaActivationOrderInfo) -> Unit,
    )
}