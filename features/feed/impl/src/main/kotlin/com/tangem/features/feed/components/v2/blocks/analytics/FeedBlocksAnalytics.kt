package com.tangem.features.feed.components.v2.blocks.analytics

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.ui.ds2.shtorka.ShtorkaExpansion
import com.tangem.domain.wallets.analytics.WalletSettingsAnalyticEvents
import kotlinx.coroutines.flow.StateFlow

@Stable
internal class FeedBlocksAnalytics(
    private val analyticsEventHandler: AnalyticsEventHandler,
    val shtorkaExpansion: StateFlow<ShtorkaExpansion>,
) {

    fun onProductCardShown(productCard: ProductCard) {
        analyticsEventHandler.send(
            FeedBlocksAnalyticsEvent.ProductCardShown(
                productCard = productCard,
                shtorkaState = shtorkaState(),
            ),
        )
    }

    fun onProductCardClicked(productCard: ProductCard) {
        analyticsEventHandler.send(
            FeedBlocksAnalyticsEvent.ProductCardClicked(
                productCard = productCard,
                shtorkaState = shtorkaState(),
            ),
        )
    }

    fun onForYouClicked() {
        analyticsEventHandler.send(FeedBlocksAnalyticsEvent.ForYouClicked(shtorkaState = shtorkaState()))
    }

    fun onAddAccountClicked() {
        analyticsEventHandler.send(
            WalletSettingsAnalyticEvents.ButtonAddAccount(
                source = AnalyticsParam.ScreensSources.Markets,
                shtorkaState = shtorkaState(),
            ),
        )
    }

    private fun shtorkaState(): AnalyticsParam.ShtorkaState = when (shtorkaExpansion.value) {
        ShtorkaExpansion.PILL -> AnalyticsParam.ShtorkaState.Collapsed
        ShtorkaExpansion.PARTIAL -> AnalyticsParam.ShtorkaState.Half
        ShtorkaExpansion.FULL -> AnalyticsParam.ShtorkaState.Full
    }
}