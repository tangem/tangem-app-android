package com.tangem.features.marketing.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.essenty.lifecycle.doOnResume
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.marketing.api.MarketingBannerComponent
import com.tangem.features.marketing.impl.model.MarketingBannerModel
import com.tangem.features.marketing.impl.ui.CtaMarketingBannerRows
import com.tangem.features.marketing.impl.ui.LinkedMarketingBanner
import com.tangem.features.marketing.impl.ui.MarketingBannerContent
import com.tangem.features.marketing.impl.ui.state.MarketingBannerListUM
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultMarketingBannerComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: MarketingBannerComponent.Params,
) : MarketingBannerComponent, AppComponentContext by appComponentContext {

    private val model: MarketingBannerModel = getOrCreateModel(params)

    init {
        lifecycle.doOnResume { model.onResume() }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        when (params) {
            is MarketingBannerComponent.Params.Cta -> CtaMarketingBannerRows(
                state = state,
                onBannerClick = model::onBannerClick,
                onDismiss = model::onDismiss,
                modifier = modifier,
            )
            is MarketingBannerComponent.Params.Standalone,
            is MarketingBannerComponent.Params.Linked,
            -> MarketingBannerContent(
                state = state,
                onBannerClick = model::onBannerClick,
                onDismiss = model::onDismiss,
                modifier = modifier,
            )
        }
    }

    @Composable
    override fun LinkedContent(providerId: String, modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val banner = (state as? MarketingBannerListUM.Content)
            ?.banners
            ?.firstOrNull { providerId in it.providerIds }
            ?: return
        LinkedMarketingBanner(
            banner = banner,
            onClick = { model.onBannerClick(banner.deeplink) },
            modifier = modifier,
        )
    }

    @Composable
    override fun hasLinkedBanner(providerId: String): Boolean {
        val state by model.uiState.collectAsStateWithLifecycle()
        return (state as? MarketingBannerListUM.Content)?.banners?.any { providerId in it.providerIds } == true
    }

    @AssistedFactory
    interface Factory : MarketingBannerComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: MarketingBannerComponent.Params,
        ): DefaultMarketingBannerComponent
    }
}