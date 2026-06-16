package com.tangem.features.tangempay.limit.setup

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.tangempay.components.TangemPayDetailsContainerComponent

internal class TangemPayCardLimitSetupComponent(
    appComponentContext: AppComponentContext,
    params: TangemPayDetailsContainerComponent.Params,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    private val model: TangemPayCardLimitSetupModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        BackHandler(onBack = router::pop)
        if (model.inRedesignEnabled()) {
            TangemPayCardLimitSetupScreenV2(
                state = state,
                modifier = modifier,
            )
        } else {
            TangemPayCardLimitSetupScreen(
                state = state,
                modifier = modifier,
            )
        }
    }
}