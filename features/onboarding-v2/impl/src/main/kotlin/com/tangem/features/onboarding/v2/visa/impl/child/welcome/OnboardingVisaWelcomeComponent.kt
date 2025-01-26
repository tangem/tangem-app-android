package com.tangem.features.onboarding.v2.visa.impl.child.welcome

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.onboarding.v2.visa.impl.child.welcome.ui.OnboardingVisaWelcome
import com.tangem.features.onboarding.v2.visa.impl.child.welcome.ui.state.OnboardingVisaWelcomeUM

internal class OnboardingVisaWelcomeComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    @Composable
    override fun Content(modifier: Modifier) {
        val state = remember(this) {
            OnboardingVisaWelcomeUM(
                mode = if (params.isWelcomeBack) {
                    OnboardingVisaWelcomeUM.Mode.WelcomeBack
                } else {
                    OnboardingVisaWelcomeUM.Mode.Hello
                },
                userName = "User", // TODO:
                onContinueClick = params.onDone,
            )
        }

        BackHandler(onBack = remember(this) { { params.onBack() } })

        OnboardingVisaWelcome(
            modifier = modifier,
            state = state,
        )
    }

    data class Params(
        val isWelcomeBack: Boolean,
        val onBack: () -> Unit,
        val onDone: () -> Unit,
    )
}
