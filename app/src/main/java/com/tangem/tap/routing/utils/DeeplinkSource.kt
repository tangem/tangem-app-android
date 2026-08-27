package com.tangem.tap.routing.utils

/**
 * Where a deeplink came from. Decides what happens to a URI that no handler routes.
 */
internal enum class DeeplinkSource {

    /**
     * Delivered by the system as `intent.data` — a browser link, an app link or another app's intent.
     * A URI that matches no route is dropped: opening it in a browser would fire an `ACTION_VIEW` that the
     * app's own BROWSABLE filters resolve back into [com.tangem.tap.MainActivity], i.e. an endless bounce.
     */
    External,

    /**
     * Reconstructed from a push payload ([com.tangem.common.routing.deeplink.PayloadToDeeplinkConverter]).
     * A URI that matches no route may still be a marketing web page, so it falls back to the browser when
     * its host is trusted.
     */
    Push,
}