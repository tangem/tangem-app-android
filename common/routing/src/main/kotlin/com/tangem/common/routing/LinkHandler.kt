package com.tangem.common.routing

import android.net.Uri
import timber.log.Timber

/**
 * Routes in-app content links (deep links and external URLs)
 * - `tangem://` scheme → parsed to AppRoute and pushed via AppRouter
 * - `https://` / `http://` → opened in external browser via UrlOpener
 * - Unknown scheme → logged and ignored
 */
class LinkHandler(
    private val appRouter: AppRouter,
) {

    fun navigate(link: String) {
        val uri = Uri.parse(link)
        handleTangemDeepLink(uri)
    }

    private fun handleTangemDeepLink(uri: Uri) {
        val route = parseDeepLinkToRoute(uri)
        if (route != null) {
            appRouter.push(route)
        } else {
            Timber.w("ContentLinkHandler: unrecognized tangem deep link: %s", uri)
        }
    }

    @Suppress("UnusedParameter", "FunctionOnlyReturningConstant")
    private fun parseDeepLinkToRoute(uri: Uri): AppRoute? {
        // TODO [REDACTED_TASK_KEY] refactor deepling routing
        return null
    }
}