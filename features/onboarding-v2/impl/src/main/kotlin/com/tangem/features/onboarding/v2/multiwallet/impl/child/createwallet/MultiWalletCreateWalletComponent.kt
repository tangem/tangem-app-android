package com.tangem.features.onboarding.v2.multiwallet.impl.child.createwallet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildParams
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.child.createwallet.model.MultiWalletCreateWalletModel
import com.tangem.features.onboarding.v2.multiwallet.impl.child.createwallet.ui.MultiWalletCreateWallet
import com.tangem.features.onboarding.v2.multiwallet.impl.model.OnboardingMultiWalletState
import kotlinx.coroutines.launch

class MultiWalletCreateWalletComponent(
    context: AppComponentContext,
    params: MultiWalletChildParams,
    onNextStep: (OnboardingMultiWalletState.Step) -> Unit,
) : AppComponentContext by context, MultiWalletChildComponent {

    private val model: MultiWalletCreateWalletModel = getOrCreateModel(params)

    init {
        params.parentParams.titleProvider.changeTitle(
            text = resourceReference(R.string.onboarding_create_wallet_header),
        )

        componentScope.launch {
            model.onDone.collect(onNextStep)
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        MultiWalletCreateWallet(
            modifier = modifier,
            state = state,
        )
    }
}
