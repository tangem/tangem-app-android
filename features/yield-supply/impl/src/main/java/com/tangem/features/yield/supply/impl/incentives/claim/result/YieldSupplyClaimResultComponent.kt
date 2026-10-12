package com.tangem.features.yield.supply.impl.incentives.claim.result

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.essenty.backhandler.BackCallback
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.yield.supply.impl.incentives.claim.result.entity.YieldSupplyClaimResultUM
import com.tangem.features.yield.supply.impl.incentives.claim.result.ui.YieldSupplyClaimResultContent
import kotlinx.coroutines.flow.StateFlow

/**
 * Shows what became of a signed claim.
 *
 * The state is owned by the active screen rather than by a model of its own: the claim is followed after this sheet
 * could have been closed, and the sheet must be able to open again on whatever the follow-up found.
 */
internal class YieldSupplyClaimResultComponent(
    private val appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    init {
        // The slot's own back handling would close the slot behind the active screen's back, leaving the result it
        // owns on record; the result is closed through the screen instead, so both go together.
        backHandler.register(BackCallback { params.onDismiss() })
    }

    override fun dismiss() {
        params.onDismiss()
    }

    /**
     * Drawn full-bleed rather than in a sheet: the design-system status template brings its own background, ambient
     * light and glow ring, all sized to the screen. It still travels in the active screen's slot, so the claim
     * outcome dismisses it the way it dismisses the sheets beside it.
     */
    @Composable
    override fun BottomSheet() {
        val state by params.state.collectAsStateWithLifecycle()
        val result = state ?: return

        YieldSupplyClaimResultContent(
            state = result,
            onGoToTokenClick = params.onGoToTokenClick,
            onGoToYieldClick = params.onGoToYieldClick,
        )
    }

    data class Params(
        val state: StateFlow<YieldSupplyClaimResultUM?>,
        val onGoToTokenClick: () -> Unit,
        val onGoToYieldClick: () -> Unit,
        val onDismiss: () -> Unit,
    )
}