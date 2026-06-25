package com.tangem.feature.usedesk.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface UsedeskComponent : ComposableContentComponent {

    data class Params(
        /** User wallet id (hex string) sent to Usedesk as the client email to identify the user. */
        val userWalletId: String? = null,
    )

    interface Factory : ComponentFactory<Params, UsedeskComponent>
}