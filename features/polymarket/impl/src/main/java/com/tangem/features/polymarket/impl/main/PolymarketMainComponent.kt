package com.tangem.features.polymarket.impl.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.essenty.lifecycle.doOnPause
import com.arkivanov.essenty.lifecycle.doOnResume
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.polymarket.impl.main.model.PolymarketMainModel
import com.tangem.features.polymarket.impl.main.model.PolymarketMainParams
import com.tangem.features.polymarket.impl.main.ui.PolymarketMainScreen

/**
 * Discovery feed screen.
 *

 * factory — its model is resolved from the model map by [getOrCreateModel].
 *
 * @param userWalletId wallet the feature was opened for; the model reads it back out of its params container,
 *  so it must be handed over here.
 *  container, so it must be handed over here.
 */
internal class PolymarketMainComponent(
    appComponentContext: AppComponentContext,
    private val userWalletId: UserWalletId,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: PolymarketMainModel = getOrCreateModel(
        params = PolymarketMainParams(userWalletId = userWalletId),
    )

    init {
        // Both an app sent to the background and an event opened on top of the feed pause the component, and
        // neither is a moment to keep refreshing what nobody is looking at.
        lifecycle.doOnResume { model.setInForeground(isInForeground = true) }
        lifecycle.doOnPause { model.setInForeground(isInForeground = false) }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        PolymarketMainScreen(
            state = state,
            onBackClick = model::onBackClick,
            onLoadMore = model::onLoadMore,
            onVisibleEventsChange = model::onVisibleEventsChange,
            onScrollIdle = model::onScrollIdle,
            modifier = modifier,
        )
    }
}