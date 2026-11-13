package com.tangem.features.send.api.mcp

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId

/** The offer an AI agent raised to send, and the hand-over to Send that carries it out. */
interface SendIntentComponent : ComposableContentComponent {

    data class Params(val intentId: String, val walletId: UserWalletId? = null)

    interface Factory : ComponentFactory<Params, SendIntentComponent>
}