package com.tangem.features.tangempay.components

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.security.DisableScreenshotsDisposableEffect
import com.tangem.features.tangempay.model.TangemPayChangePinModel
import com.tangem.features.tangempay.ui.TangemPayChangePinScreen
import com.tangem.features.tangempay.ui.TangemPayChangePinScreenV2

internal class TangemPayChangePinComponent(
    private val appComponentContext: AppComponentContext,
    params: TangemPayDetailsContainerComponent.Params,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    private val model: TangemPayChangePinModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        BackHandler(onBack = router::pop)
        DisableScreenshotsDisposableEffect()
        if (model.isRedesignEnabled()) {
            TangemPayChangePinScreenV2(
                state = state,
                onBackClick = router::pop,
            )
        } else {
            TangemPayChangePinScreen(
                state = state,
                onBackClick = router::pop,
            )
        }
    }
}