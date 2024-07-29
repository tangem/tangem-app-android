package com.tangem.features.markets

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.domain.markets.TokenMarket
import com.tangem.features.markets.details.api.MarketsTokenDetailsComponent
import com.tangem.features.markets.tokenlist.api.MarketsTokenListComponent
import kotlinx.serialization.Serializable
import javax.inject.Inject

internal class MarketsEntryChildFactory @Inject constructor(
    private val tokenListComponentFactory: MarketsTokenListComponent.Factory,
    private val tokenDetailsComponentFactory: MarketsTokenDetailsComponent.Factory,
) {

    @Serializable
    sealed interface Child {

        @Serializable
        data object TokenList : Child

        @Serializable
        data class TokenDetails(val params: MarketsTokenDetailsComponent.Params) : Child
    }

    fun createChild(
        child: Child,
        appComponentContext: AppComponentContext,
        onTokenSelected: (TokenMarket) -> Unit,
    ): Any {
        return when (child) {
            is Child.TokenDetails -> {
                tokenDetailsComponentFactory.create(
                    context = appComponentContext,
                    params = child.params,
                )
            }
            is Child.TokenList -> {
                tokenListComponentFactory.create(
                    context = appComponentContext,
                    onTokenSelected = onTokenSelected,
                )
            }
        }
    }
}