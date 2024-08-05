package com.tangem.features.markets

import androidx.compose.runtime.Immutable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.domain.appcurrency.model.AppCurrency
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
    @Immutable
    sealed interface Child {

        @Serializable
        @Immutable
        data object TokenList : Child

        @Serializable
        @Immutable
        data class TokenDetails(val params: MarketsTokenDetailsComponent.Params) : Child
    }

    fun createChild(
        child: Child,
        appComponentContext: AppComponentContext,
        onTokenSelected: (TokenMarket, AppCurrency) -> Unit,
        onDetailsBack: () -> Unit,
    ): Any {
        return when (child) {
            is Child.TokenDetails -> {
                tokenDetailsComponentFactory.create(
                    context = appComponentContext,
                    params = child.params,
                    onBack = onDetailsBack,
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
