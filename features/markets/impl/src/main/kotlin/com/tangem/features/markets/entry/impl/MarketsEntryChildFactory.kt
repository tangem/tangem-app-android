package com.tangem.features.markets.entry.impl

import androidx.compose.runtime.Immutable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.navigation.Route
import com.tangem.features.markets.details.MarketsTokenDetailsComponent
import com.tangem.features.markets.tokenlist.MarketsTokenListComponent
import kotlinx.serialization.Serializable
import javax.inject.Inject

internal class MarketsEntryChildFactory @Inject constructor(
    private val tokenListComponentFactory: MarketsTokenListComponent.Factory,
    private val tokenDetailsComponentFactory: MarketsTokenDetailsComponent.Factory,
) {

    @Serializable
    @Immutable
    sealed interface Child : Route {

        @Serializable
        @Immutable
        data object TokenList : Child

        @Serializable
        @Immutable
        data class TokenDetails(val params: MarketsTokenDetailsComponent.Params) : Child
    }

    fun createChild(child: Child, appComponentContext: AppComponentContext): Any {
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
                    params = Unit,
                )
            }
        }
    }
}