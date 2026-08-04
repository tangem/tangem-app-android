package com.tangem.features.polymarket.impl.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.polymarket.api.PolymarketComponent
import com.tangem.features.polymarket.impl.onboarding.model.PolymarketOnboardingModel
import com.tangem.features.polymarket.impl.onboarding.ui.PolymarketOnboardingScreen

/**
 * Entry gate of the feature.
 *

 * assisted factory — its model is resolved from the model map by [getOrCreateModel].
 *
 * @param params feature params the gate resolves the entry decision for; the model reads them back out of its
 *  params container, so they must be handed over here.
 */
internal class PolymarketOnboardingComponent(
    appComponentContext: AppComponentContext,
    params: PolymarketComponent.Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: PolymarketOnboardingModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        PolymarketOnboardingScreen(
            state = state,
            onCloseClick = model::onCloseClick,
            modifier = modifier,
        )
    }
}