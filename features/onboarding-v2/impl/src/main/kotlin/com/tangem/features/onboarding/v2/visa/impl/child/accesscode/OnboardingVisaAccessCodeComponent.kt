package com.tangem.features.onboarding.v2.visa.impl.child.accesscode

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.onboarding.v2.visa.impl.child.accesscode.model.OnboardingVisaAccessCodeModel
import com.tangem.features.onboarding.v2.visa.impl.child.accesscode.ui.OnboardingVisaAccessCode
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

internal class OnboardingVisaAccessCodeComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: OnboardingVisaAccessCodeModel = getOrCreateModel()

    init {
        componentScope.launch {
            model.onBack.collect { params.onBack() }
        }
        componentScope.launch {
            params.parentBackEvent.collect {
                model.onBack()
            }
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        BackHandler(onBack = model::onBack)

        OnboardingVisaAccessCode(state, modifier)
    }

    data class Params(
        val parentBackEvent: SharedFlow<Unit>,
        val onBack: () -> Unit,
    )
}