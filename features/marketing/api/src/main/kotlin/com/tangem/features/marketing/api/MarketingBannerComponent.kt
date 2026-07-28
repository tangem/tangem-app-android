package com.tangem.features.marketing.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import kotlinx.coroutines.flow.Flow

interface MarketingBannerComponent : ComposableContentComponent {

    /**
     * Renders the LINKED_TO_PROVIDER banner for the offer identified by [providerId] (the row this sits
     * next to). Shows nothing when no linked campaign targets that provider. No-op for standalone banners.
     */
    @Composable
    fun LinkedContent(providerId: String, modifier: Modifier) {
        // Default no-op: only the LINKED_TO_PROVIDER implementation renders a banner.
    }

    /**
     * Whether a LINKED_TO_PROVIDER banner is available for [providerId]. The host uses this to glue the
     * banner to the offer (e.g. squaring the offer's bottom corners). Always `false` for standalone banners.
     */
    @Composable
    fun hasLinkedBanner(providerId: String): Boolean = false

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

        /** LINKED single banner rendered inline next to a host item (currently an onramp provider offer). */
        data class Linked(val requestFlow: Flow<LinkedBannerRequest?>) : Params
    }

    interface Factory : ComponentFactory<Params, MarketingBannerComponent>
}