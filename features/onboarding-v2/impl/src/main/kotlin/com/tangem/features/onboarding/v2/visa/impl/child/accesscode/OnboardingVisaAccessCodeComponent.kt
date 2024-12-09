package com.tangem.features.onboarding.v2.visa.impl.child.accesscode

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.onboarding.v2.visa.impl.child.accesscode.model.OnboardingVisaAccessCodeModel
import kotlinx.coroutines.flow.SharedFlow

class OnboardingVisaAccessCodeComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: OnboardingVisaAccessCodeModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        BackHandler(onBack = model::onBack)
    }

    data class Params(
        val parentBackEvent: SharedFlow<Unit>,
        val onBack: () -> Unit,
    )
}