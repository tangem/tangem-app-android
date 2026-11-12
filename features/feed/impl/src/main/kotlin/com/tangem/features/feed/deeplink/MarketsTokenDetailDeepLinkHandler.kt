package com.tangem.features.feed.deeplink

import arrow.core.getOrElse
import com.tangem.common.routing.AppRoute
import com.tangem.domain.markets.PreselectedTokenDetailsSection
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.common.routing.deeplink.DeeplinkConst.SECTION_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TOKEN_ID_KEY
import com.tangem.data.common.currency.getTokenIconUrlFromDefaultHost
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.GetTokenMarketInfoUseCase
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class MarketsTokenDetailDeepLinkHandler @Inject constructor(
    private val appRouter: AppRouter,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getTokenMarketInfoUseCase: GetTokenMarketInfoUseCase,
) : DeepLinkHandler {

    override fun handle(request: DeepLinkRequest) {
        val tokenId = request.queryParams[TOKEN_ID_KEY]
        val section = PreselectedTokenDetailsSection.parse(request.queryParams[SECTION_KEY])

        if (tokenId.isNullOrEmpty()) {
            TangemLogger.e("Markets token details deeplink does not contain token_id")
            appRouter.push(AppRoute.Markets())
            return
        }

        val rawTokenId = CryptoCurrency.RawID(tokenId)

        request.scope.launch {
            val appCurrency = getSelectedAppCurrencyUseCase.invokeSync().getOrElse {
                AppCurrency.Default
            }
            val tokenInfo = getTokenMarketInfoUseCase(
                appCurrency = appCurrency,
                tokenId = rawTokenId,
                tokenSymbol = "", // used for analytics
            ).getOrElse {
                TangemLogger.e("Failed to get market token info")
                return@launch
            }

            appRouter.push(
                AppRoute.MarketsTokenDetails(
                    token = TokenMarketParams(
                        id = rawTokenId,
                        name = tokenInfo.name,
                        symbol = tokenInfo.symbol,
                        tokenQuotes = TokenMarketParams.Quotes(
                            currentPrice = tokenInfo.quotes.currentPrice,
                            h24Percent = tokenInfo.quotes.h24ChangePercent,
                            weekPercent = tokenInfo.quotes.weekChangePercent,
                            monthPercent = tokenInfo.quotes.monthChangePercent,
                        ),
                        imageUrl = getTokenIconUrlFromDefaultHost(rawTokenId),
                    ),
                    appCurrency = appCurrency,
                    shouldShowPortfolio = true,
                    analyticsParams = null,
                    preselectedSection = section,
                ),
            )
        }
    }

    companion object {
        const val DEEPLINK_KEY = "tangem://token_chart"
    }
}