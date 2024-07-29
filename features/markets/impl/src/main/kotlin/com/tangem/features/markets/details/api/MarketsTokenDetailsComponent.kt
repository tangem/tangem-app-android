package com.tangem.features.markets.details.api

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import kotlinx.serialization.Serializable

@Stable
interface MarketsTokenDetailsComponent : ComposableContentComponent {

    @Serializable
    data class Params(val token: TokenMarketSerializable)

    interface Factory : ComponentFactory<Params, MarketsTokenDetailsComponent>
}