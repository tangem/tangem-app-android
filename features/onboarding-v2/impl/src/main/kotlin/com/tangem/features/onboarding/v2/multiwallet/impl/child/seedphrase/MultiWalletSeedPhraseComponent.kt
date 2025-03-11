package com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.security.DisableScreenshotsDisposableEffect
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildParams
import com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.model.MultiWalletSeedPhraseModel
import com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.ui.MultiWalletSeedPhrase
import com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.ui.state.MultiWalletSeedPhraseUM
import com.tangem.features.onboarding.v2.multiwallet.impl.model.OnboardingMultiWalletState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Suppress("UnusedPrivateMember")
internal class MultiWalletSeedPhraseComponent(
    context: AppComponentContext,
    params: MultiWalletChildParams,
    onNextStep: (OnboardingMultiWalletState.Step) -> Unit,
    onBack: () -> Unit,
    backButtonClickFlow: SharedFlow<Unit>,
) : AppComponentContext by context, MultiWalletChildComponent {

    private val model: MultiWalletSeedPhraseModel = getOrCreateModel(params)

    init {
        componentScope.launch {
            model.uiState.collect {
                // change stepper state based on the stack of the current step
                @Suppress("MagicNumber")
                params.innerNavigation.update { st ->
                    st.copy(
                        stackSize = 3 + it.order,
                        stackMaxSize = 10,
                    )
                }

                val title = when (it) {
                    is MultiWalletSeedPhraseUM.Import -> R.string.onboarding_seed_intro_button_import
                    is MultiWalletSeedPhraseUM.GenerateSeedPhrase,
                    is MultiWalletSeedPhraseUM.GeneratedWordsCheck,
                    is MultiWalletSeedPhraseUM.Start,
                    -> R.string.onboarding_create_wallet_header
                }

                params.parentParams.titleProvider.changeTitle(text = resourceReference(title))
            }
        }

        componentScope.launch {
            backButtonClickFlow.collect { model.onBack() }
        }
        componentScope.launch {
            model.onDone.collect { onNextStep(OnboardingMultiWalletState.Step.AddBackupDevice) }
        }
        componentScope.launch {
            model.navigateBack.collect { onBack() }
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        BackHandler(enabled = true) {
            model.onBack()
        }

        DisableScreenshotsDisposableEffect()

        MultiWalletSeedPhrase(modifier = modifier, state = state)
    }
}