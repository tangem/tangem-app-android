package com.tangem.features.markets.token.block

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import kotlinx.serialization.Serializable

@Stable
interface TokenMarketBlockComponent : ComposableContentComponent {

    @Serializable
    data class Params(
        val tokenId: String,
        val tokenName: String,
        val tokenSymbol: String,
        val tokenImageUrl: String?,
    )

    interface Factory {
        fun create(appComponentContext: AppComponentContext, params: Params): TokenMarketBlockComponent
    }
}