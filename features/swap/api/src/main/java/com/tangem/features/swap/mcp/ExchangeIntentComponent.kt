package com.tangem.features.swap.mcp

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

/** The offer an AI agent raised to swap, and the hand-over to Swap that carries it out. */
interface ExchangeIntentComponent : ComposableContentComponent {

    data class Params(val intentId: String)

    interface Factory : ComponentFactory<Params, ExchangeIntentComponent>
}