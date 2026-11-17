package com.tangem.features.swap.mcp

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId

/** Presents an AI agent's swap intent or its status and hands accepted intents over to Swap. */
interface ExchangeIntentComponent : ComposableContentComponent {

    data class Params(val intentId: String, val walletId: UserWalletId? = null)

    interface Factory : ComponentFactory<Params, ExchangeIntentComponent>
}