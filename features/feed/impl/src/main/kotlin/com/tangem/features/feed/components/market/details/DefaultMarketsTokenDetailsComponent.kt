package com.tangem.features.feed.components.market.details

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableModularContentComponent
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.TokenMarketParams
import kotlinx.serialization.Serializable

internal class DefaultMarketsTokenDetailsComponent(
    appComponentContext: AppComponentContext,
    val params: Params,
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

    @Serializable
    data class Params(
        val token: TokenMarketParams,
        val appCurrency: AppCurrency,
        val shouldShowPortfolio: Boolean,
        val analyticsParams: AnalyticsParams?,
    )

    @Serializable
    data class AnalyticsParams(
        val blockchain: String?,
        val source: String,
    )
}