package com.tangem.features.onboarding.v2.multiwallet.impl.child.finalize

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildParams
import com.tangem.features.onboarding.v2.multiwallet.impl.child.finalize.model.MultiWalletFinalizeModel
import com.tangem.features.onboarding.v2.multiwallet.impl.child.finalize.ui.MultiWalletFinalize
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Suppress("UnusedPrivateMember")
internal class MultiWalletFinalizeComponent(
    context: AppComponentContext,
    params: MultiWalletChildParams,
    backButtonClickFlow: SharedFlow<Unit>,
    onBack: () -> Unit,
    onEvent: (Event) -> Unit,
) : AppComponentContext by context, MultiWalletChildComponent {

    private val model: MultiWalletFinalizeModel = getOrCreateModel(params)

    init {
        params.innerNavigation.update {
            it.copy(
                stackSize = 7,
                stackMaxSize = 9,
            )
        }
        params.parentParams.titleProvider.changeTitle(
            resourceReference(R.string.onboarding_button_finalize_backup),
        )

        componentScope.launch {
            model.onEvent.collect {
                onEvent(it)
            }
        }

        componentScope.launch {
            backButtonClickFlow.collect { model.onBack() }
        }
        componentScope.launch {
            model.onBackFlow.collect { onBack() }
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        BackHandler { model.onBack() }

        MultiWalletFinalize(
            state = state,
            modifier = modifier,
        )
    }

    enum class Event {
        OneBackupCardAdded, TwoBackupCardsAdded, ThreeBackupCardsAdded
    }
}