package com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildParams
import com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.model.MultiWalletSeedPhraseModel
import com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.ui.MultiWalletSeedPhrase
import com.tangem.features.onboarding.v2.multiwallet.impl.model.OnboardingMultiWalletState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

@Suppress("UnusedPrivateMember")
class MultiWalletSeedPhraseComponent(
    context: AppComponentContext,
    params: MultiWalletChildParams,
    onNextStep: (OnboardingMultiWalletState.Step) -> Unit,
    backButtonClickFlow: SharedFlow<Unit>,
) : AppComponentContext by context, MultiWalletChildComponent {

    private val model: MultiWalletSeedPhraseModel = getOrCreateModel()

    init {
        componentScope.launch {
            backButtonClickFlow.collect {
                model.onBack()
            }
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        BackHandler(enabled = true) {
            model.onBack()
        }

        MultiWalletSeedPhrase(
            modifier = modifier,
            state = state,
        )
    }
}