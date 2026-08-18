package com.tangem.features.polymarket.impl.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.polymarket.impl.onboarding.model.PolymarketOnboardingModel
import com.tangem.features.polymarket.impl.onboarding.model.PolymarketOnboardingParams
import com.tangem.features.polymarket.impl.onboarding.ui.PolymarketOnboardingScreen

/**
 * Entry gate of the feature.
 *

 * assisted factory — its model is resolved from the model map by [getOrCreateModel].
 *
 * @param userWalletId the wallet `PolymarketRoute.Entry` settled; the model reads it back out of its params
 *  container, so it must be handed over here.
 */
internal class PolymarketOnboardingComponent(
    appComponentContext: AppComponentContext,
    userWalletId: UserWalletId,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: PolymarketOnboardingModel = getOrCreateModel(
        params = PolymarketOnboardingParams(userWalletId = userWalletId),
    )

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