package com.tangem.features.polymarket.impl.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.polymarket.impl.details.model.PolymarketEventDetailsModel
import com.tangem.features.polymarket.impl.details.ui.PolymarketEventDetailsScreen

/**
 * Event details screen, pushed onto the feature stack over the Discovery feed.
 *

 * factory — its model is resolved from the model map by [getOrCreateModel].
 *
 * An outcome tap opens the place-prediction flow as a route on the feature stack rather than as a sheet
 * hosted here: its screens are full-screen, and hosting them under a screen that is itself a stack entry
 * would nest one flow inside another for no reason.
 */
internal class PolymarketEventDetailsComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: PolymarketEventDetailsModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        PolymarketEventDetailsScreen(
            state = state,
            onBackClick = model::onBackClick,
            modifier = modifier,
        )
    }

    /**
     * @property eventId event to show
     * @property userWalletId wallet the feature was opened for, carried into the place-prediction flow the
     *  screen starts.
     * @property marketId market preselected by the caller, e.g. by tapping an outcome on the feed card
     * @property assetId outcome preselected by the caller
     */
    data class Params(
        val eventId: String,
        val userWalletId: UserWalletId,
        val marketId: String? = null,
        val assetId: String? = null,
    )
}