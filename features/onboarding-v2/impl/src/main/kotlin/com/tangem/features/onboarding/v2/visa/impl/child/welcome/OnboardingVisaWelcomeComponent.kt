package com.tangem.features.onboarding.v2.visa.impl.child.welcome

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent

class OnboardingVisaWelcomeComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    @Composable
    override fun Content(modifier: Modifier) {
        BackHandler(onBack = remember(this) { { params.onBack() } })
    }

    data class Params(
        val onBack: () -> Unit,
    )
}