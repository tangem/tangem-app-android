package com.tangem.features.tangempay.components

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.res.LocalVisaRedesignEnabled
import com.tangem.features.tangempay.model.TangemPayAddToWalletModel
import com.tangem.features.tangempay.ui.TangemPayAddToWalletScreen
import com.tangem.features.tangempay.ui.TangemPayAddToWalletScreenV2

internal class TangemPayAddToWalletComponent(
    private val appComponentContext: AppComponentContext,
    private val params: TangemPayDetailsContainerComponent.Params,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    private val model: TangemPayAddToWalletModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val cardDetailsState by model.cardDetailsState.collectAsStateWithLifecycle()
        BackHandler(onBack = router::pop)
        CompositionLocalProvider(LocalVisaRedesignEnabled provides model.isRedesignEnabled()) {
            if (model.isRedesignEnabled()) {
                TangemPayAddToWalletScreenV2(
                    state = state,
                    cardDetailsState = cardDetailsState,
                )
            } else {
                TangemPayAddToWalletScreen(
                    state = state,
                    cardDetailsState = cardDetailsState,
                )
            }
        }
    }
}