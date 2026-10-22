package com.tangem.core.navigation.deeplink

/**
 * Routes in-app content links to the appropriate destination.
 * - Deep link schemes (e.g. `tangem://`, `wc://`) → handled in-app
 * - Web URLs (`https://`) → opened in browser
 */
interface DeeplinkLauncher {

    /**
     * @param shouldReportFailure whether a [link] that can't be opened is sent to analytics. A link supplied by the
     * backend (a banner) that fails is a bug worth reporting; arbitrary user input (a scanned QR code) is not.
     */
    fun launch(link: String, shouldReportFailure: Boolean = true)
}