package com.tangem.features.feed.components.market.details.portfolio.add.impl.model

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.feed.components.market.details.portfolio.add.impl.TokenActionsComponent
import com.tangem.features.feed.components.market.details.portfolio.add.impl.ui.state.TokenActionsUM
import com.tangem.features.feed.components.market.details.portfolio.impl.loader.PortfolioData
import com.tangem.features.feed.components.market.details.portfolio.impl.model.PortfolioTokenUMConverter
import com.tangem.features.feed.components.market.details.portfolio.impl.model.TokenActionsHandler
import javax.inject.Inject

@ModelScoped
internal class TokenActionsUiBuilder @Inject constructor(
    paramsContainer: ParamsContainer,
    private val analyticsEventHandler: AnalyticsEventHandler,
) {
    private val params = paramsContainer.require<TokenActionsComponent.Params>()

    fun build(data: PortfolioData.CryptoCurrencyData, tokenActionsHandler: TokenActionsHandler): TokenActionsUM {
        val status = data.status
        val tokenUM = TokenItemState.Content(
            id = status.currency.id.value,
            iconState = CryptoCurrencyToIconStateConverter().convert(status.currency),
            titleState = TokenItemState.TitleState.Content(stringReference(status.currency.name)),
            fiatAmountState = null,
            subtitle2State = null,
            subtitleState = TokenItemState.SubtitleState.TextContent(stringReference(status.currency.symbol)),
            onItemClick = null,
            onItemLongClick = null,
        )
        return TokenActionsUM(
            token = tokenUM,
            onLaterClick = {
                analyticsEventHandler.send(params.eventBuilder.getTokenLater())
                params.callbacks.onLaterClick()
            },
            quickActions = PortfolioTokenUMConverter.quickActions(data, tokenActionsHandler),
        )
    }
}