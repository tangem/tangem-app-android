package com.tangem.features.polymarket.impl.category

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
import com.tangem.domain.polymarket.model.PolymarketCategory
import com.tangem.features.polymarket.impl.category.model.PolymarketCategoryModel
import com.tangem.features.polymarket.impl.category.ui.PolymarketCategoryScreen

internal class PolymarketCategoryComponent(
    context: AppComponentContext,
    params: Params,
) : ComposableContentComponent, AppComponentContext by context {

    private val model: PolymarketCategoryModel = getOrCreateModel(params)

    init {
        lifecycle.doOnResume { model.onResume() }
        lifecycle.doOnPause { model.onPause() }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        PolymarketCategoryScreen(
            state = state,
            onBack = model::onBack,
            onLoadMore = model::onLoadMore,
            onVisibleEventsChange = model::onVisibleEventsChange,
            onScrollIdle = model::onScrollIdle,
            modifier = modifier,
        )
    }

    data class Params(val userWalletId: UserWalletId, val category: PolymarketCategory)
}