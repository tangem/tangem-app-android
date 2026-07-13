package com.tangem.features.marketing.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import kotlinx.coroutines.flow.Flow

interface MarketingBannerComponent : ComposableContentComponent {

    sealed interface Params {

        /**
         * STANDALONE carousel; hosted on all 6 screens. `null` in the flow hides the banner.
         *
         * @param onDeeplinkClick optional interceptor for a tapped banner deeplink. Return `true` when
         * the host routed it contextually (e.g. `tangem://swap`/`tangem://buy` for the current token);
         * `false`/`null` lets the banner fall back to the generic deeplink launcher (external links).
         */
        data class Standalone(
            val requestFlow: Flow<MarketingBannerRequest?>,
            val onDeeplinkClick: ((deeplink: String) -> Boolean)? = null,
        ) : Params

        /** LINKED_TO_PROVIDER single banner rendered inline next to an onramp provider offer. */
        data class LinkedToProvider(val requestFlow: Flow<LinkedBannerRequest?>) : Params
    }

    interface Factory : ComponentFactory<Params, MarketingBannerComponent>
}