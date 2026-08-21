package com.tangem.features.polymarket.impl.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.polymarket.impl.search.model.PolymarketSearchModel

/**
 * Full-text search over discoverable events. Placeholder rendering; the real screen arrives with the
 * UI part of the feature.
 */
internal class PolymarketSearchComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: PolymarketSearchModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Text(text = "Search: ${state.query}")
        }
    }

    /**
     * @property userWalletId the wallet the feature runs for, carried into the details of a tapped result
     */
    data class Params(
        val userWalletId: UserWalletId,
    )
}