package com.tangem.features.onboarding.v2.multiwallet.impl.child.finalize

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildParams
import com.tangem.features.onboarding.v2.multiwallet.impl.child.finalize.model.MultiWalletFinalizeModel
import com.tangem.features.onboarding.v2.multiwallet.impl.child.finalize.ui.MultiWalletFinalize
import kotlinx.coroutines.launch

@Suppress("UnusedPrivateMember")
internal class MultiWalletFinalizeComponent(
    context: AppComponentContext,
    params: MultiWalletChildParams,
    onEvent: (Event) -> Unit,
) : AppComponentContext by context, MultiWalletChildComponent {

    private val model: MultiWalletFinalizeModel = getOrCreateModel(params)

    init {
        componentScope.launch {
            model.onEvent.collect {
                onEvent(it)
            }
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        MultiWalletFinalize(
            state = state,
            modifier = modifier,
        )
    }

    enum class Event {
        OneBackupCardAdded, TwoBackupCardsAdded, ThreeBackupCardsAdded
    }
}
