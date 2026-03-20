package com.tangem.core.navigation.deeplink

/**
 * Routes in-app content links to the appropriate destination.
 * - Deep link schemes (e.g. `tangem://`, `wc://`) → handled in-app
 * - Web URLs (`https://`) → opened in browser
 */
interface DeeplinkLauncher {

    fun launch(link: String)
}