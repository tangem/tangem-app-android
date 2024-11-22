package com.tangem.features.onboarding.v2.multiwallet.impl.child.finalize

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildParams
import com.tangem.features.onboarding.v2.multiwallet.impl.model.OnboardingMultiWalletState
import kotlinx.coroutines.flow.SharedFlow

@Suppress("UnusedPrivateMember")
internal class MultiWalletFinalizeComponent(
    context: AppComponentContext,
    params: MultiWalletChildParams,
    onNextStep: (OnboardingMultiWalletState.Step) -> Unit,
    backButtonClickFlow: SharedFlow<Unit>,
) : AppComponentContext by context, MultiWalletChildComponent {

    @Composable
    override fun Content(modifier: Modifier) {
    }
}
