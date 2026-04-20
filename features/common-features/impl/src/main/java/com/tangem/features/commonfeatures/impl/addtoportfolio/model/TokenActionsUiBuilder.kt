package com.tangem.features.commonfeatures.impl.addtoportfolio.model

import com.tangem.common.ui.markets.action.CryptoCurrencyData
import com.tangem.common.ui.markets.action.QuickActionsConverter.quickActions
import com.tangem.common.ui.markets.action.TokenActionsHandler
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.commonfeatures.impl.addtoportfolio.TokenActionsComponent
import com.tangem.features.commonfeatures.impl.addtoportfolio.analytics.PortfolioAnalyticsEvent
import com.tangem.features.commonfeatures.impl.addtoportfolio.ui.state.TokenActionsUM
import javax.inject.Inject

@ModelScoped
internal class TokenActionsUiBuilder @Inject constructor(
    paramsContainer: ParamsContainer,
    private val analyticsEventHandler: AnalyticsEventHandler,
) {
    private val params = paramsContainer.require<TokenActionsComponent.Params>()

    fun build(
        data: CryptoCurrencyData,
        tokenActionsHandler: TokenActionsHandler,
        eventBuilder: PortfolioAnalyticsEvent.EventBuilder,
    ): TokenActionsUM {
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
                analyticsEventHandler.send(eventBuilder.getTokenLater())
                params.callbacks.onLaterClick()
            },
            quickActions = quickActions(data, tokenActionsHandler),
        )
    }
}