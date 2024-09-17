package com.tangem.features.markets.token.block

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.tokens.model.CryptoCurrency
import kotlinx.serialization.Serializable

@Stable
interface TokenMarketBlockComponent : ComposableContentComponent {

    @Serializable
    data class Params(
        val cryptoCurrency: CryptoCurrency,
    )

    interface Factory {
        fun create(appComponentContext: AppComponentContext, params: Params): TokenMarketBlockComponent
    }
}