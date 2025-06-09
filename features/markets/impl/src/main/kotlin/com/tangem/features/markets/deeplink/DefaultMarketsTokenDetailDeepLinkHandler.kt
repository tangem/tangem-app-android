package com.tangem.features.markets.deeplink

import arrow.core.getOrElse
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.data.common.currency.getTokenIconUrlFromDefaultHost
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.GetTokenMarketInfoUseCase
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.domain.models.currency.CryptoCurrency
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

internal class DefaultMarketsTokenDetailDeepLinkHandler @AssistedInject constructor(
    @Assisted scope: CoroutineScope,
    @Assisted queryParams: Map<String, String>,
    appRouter: AppRouter,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    getTokenMarketInfoUseCase: GetTokenMarketInfoUseCase,
) : MarketsTokenDetailDeepLinkHandler {

    init {
        val tokenId = queryParams[TOKEN_ID_KEY]

        val rawTokenId = CryptoCurrency.RawID(tokenId.orEmpty())

        scope.launch {
            val appCurrency = getSelectedAppCurrencyUseCase.invokeSync().getOrElse {
                AppCurrency.Default
            }
            val tokenInfo = getTokenMarketInfoUseCase(
                appCurrency = appCurrency,
                tokenId = rawTokenId,
                tokenSymbol = TOKEN_SYMBOL_KEY,
            ).getOrElse {
                Timber.e("Failed to get market token info")
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
                    showPortfolio = true,
                    analyticsParams = null,
                ),
            )
        }
    }

    @AssistedFactory
    interface Factory : MarketsTokenDetailDeepLinkHandler.Factory {
        override fun create(
            coroutineScope: CoroutineScope,
            queryParams: Map<String, String>,
        ): DefaultMarketsTokenDetailDeepLinkHandler
    }

    private companion object {
        const val TOKEN_ID_KEY = "token_id"
        const val TOKEN_SYMBOL_KEY = "token_symbol"
    }
}