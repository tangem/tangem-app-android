package com.tangem.features.foryou

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableModularBottomSheetContentComponent
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.models.currency.CryptoCurrency
import kotlinx.serialization.Serializable

interface TokenSummaryComponent : ComposableModularBottomSheetContentComponent {

    data class Params(
        val token: Token,
        val selectedTokenPeriodId: String? = null,
        val callbacks: TokenSummaryModelCallbacks,
    )

    interface TokenSummaryModelCallbacks {
        fun onDismiss()
    }

    /**
     * The token the summary is opened for. Has two shapes depending on the entry point:
     *  - [Portfolio] — opened from a portfolio screen, where the full [CryptoCurrency] is available;
     *  - [Market] — opened from a market-review screen, where there is no [CryptoCurrency] yet, only the
     *  raw id and display data.
     */
    @Serializable
    sealed interface Token {

        @Serializable
        data class Portfolio(val cryptoCurrency: CryptoCurrency) : Token

        /**
         * @property networks networks the token can be added to, already filtered by the caller. Empty when the
         * caller has not resolved them yet, which leaves the token unaddable for the lifetime of the summary.
         */
        @Serializable
        data class Market(
            val cryptoCurrencyRawId: CryptoCurrency.RawID,
            val symbol: String,
            val title: String,
            val tangemIconUrl: String,
            val networks: List<TokenMarketInfo.Network> = emptyList(),
        ) : Token
    }

    interface Factory : ComponentFactory<Params, TokenSummaryComponent>
}