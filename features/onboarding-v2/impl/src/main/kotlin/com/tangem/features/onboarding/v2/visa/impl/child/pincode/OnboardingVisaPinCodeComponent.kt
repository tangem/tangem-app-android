package com.tangem.features.onboarding.v2.visa.impl.child.pincode

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.security.DisableScreenshotsDisposableEffect
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.visa.model.VisaActivationOrderInfo
import com.tangem.features.onboarding.v2.visa.impl.DefaultOnboardingVisaComponent
import com.tangem.features.onboarding.v2.visa.impl.child.pincode.model.OnboardingVisaPinCodeModel
import com.tangem.features.onboarding.v2.visa.impl.child.pincode.ui.OnboardingVisaPinCode
import kotlinx.coroutines.launch

internal class OnboardingVisaPinCodeComponent(
    appComponentContext: AppComponentContext,
    config: Config,
    private val params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: OnboardingVisaPinCodeModel = getOrCreateModel(config)

    init {
        componentScope.launch {
            model.onDone.collect { params.onDone() }
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        BackHandler(onBack = remember(this) { { params.childParams.onBack() } })

        DisableScreenshotsDisposableEffect()

        OnboardingVisaPinCode(
            modifier = modifier,
            state = state,
        )
    }

    data class Config(
        val scanResponse: ScanResponse,
        val activationOrderInfo: VisaActivationOrderInfo,
        val wasValidationError: Boolean,
    )

    data class Params(
        val childParams: DefaultOnboardingVisaComponent.ChildParams,
        val onDone: () -> Unit,
    )
}