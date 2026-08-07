package com.tangem.features.foryou

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableModularBottomSheetContentComponent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.serialization.Serializable

interface TokenSummaryComponent : ComposableModularBottomSheetContentComponent {

    data class Params(
        val userWalletId: UserWalletId,
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

        @Serializable
        data class Market(
            val cryptoCurrencyRawId: CryptoCurrency.RawID,
            val title: String,
            val tangemIconUrl: String,
        ) : Token
    }

    interface Factory : ComponentFactory<Params, TokenSummaryComponent>
}