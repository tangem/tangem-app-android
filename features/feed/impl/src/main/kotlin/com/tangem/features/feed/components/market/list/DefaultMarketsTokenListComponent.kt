package com.tangem.features.feed.components.market.list

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableModularContentComponent
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.TokenMarketParams

@Suppress("UnusedPrivateProperty") // TODO will be remove in next PR
internal class DefaultMarketsTokenListComponent(
    appComponentContext: AppComponentContext,
    private val onTokenClick: ((TokenMarketParams, AppCurrency) -> Unit)? = null,
) : ComposableModularContentComponent, AppComponentContext by appComponentContext {

    @Composable
    override fun Title() {
    }

    @Composable
    override fun Content(modifier: Modifier) {
    }

    @Composable
    override fun Footer() {
    }
}