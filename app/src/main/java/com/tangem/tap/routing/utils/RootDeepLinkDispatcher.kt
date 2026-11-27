package com.tangem.tap.routing.utils

import android.net.Uri
import com.tangem.common.routing.AppRoute
import com.tangem.common.uri.ExternalUrlValidator
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.deeplink.DeepLinkRequest
import com.tangem.core.navigation.deeplink.DeepLinkScheme
import com.tangem.core.navigation.impl.deeplink.DeepLinkHandlerRegistry
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.data.card.sdk.CardSdkProvider
import com.tangem.utils.extensions.uriValidate
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/** Dispatches the deep link parked in [DeepLinkFactory] once the root stack is ready to route. */
internal class RootDeepLinkDispatcher @Inject constructor(
    private val deepLinkFactory: DeepLinkFactory,
    private val deepLinkHandlerRegistry: DeepLinkHandlerRegistry,
    private val cardSdkProvider: CardSdkProvider,
    private val urlOpener: UrlOpener,
) {

    /**
     * Dispatches deep links while [scope] is active, with [router] of the root stack whose active route is
     * [currentRoute].
     */
    fun observe(scope: CoroutineScope, currentRoute: Flow<AppRoute?>, router: Router) {
        combine(
            deepLinkFactory.parkedDeepLink,
            currentRoute,
            cardSdkProvider.sdk.uiVisibility(),
        ) { deepLink, route, isCardSdkVisible ->
            deepLink.takeIf { route != null && isReadyToRoute(route) && !isCardSdkVisible }
        }
            .filterNotNull()
            .onEach { deepLink ->
                // A navigation made by the handler re-emits the route before the consumed link reaches combine
                if (deepLinkFactory.consume(deepLink)) dispatch(deepLink, router)
            }
            .launchIn(scope)
    }

    private fun dispatch(deepLink: ParkedDeepLink, router: Router) {
        val isRouted = launchRegisteredDeepLink(deepLink, router)

        if (!isRouted && deepLink.source == DeeplinkSource.Push) {
            openWebLinkFromPush(deepLink.uri)
        }
    }

    /**
     * Dispatches [deepLink] to the handler a feature contributed for it. Returns `false` when no feature claims it.
     * A push deep link to a handler that is not openable from push is consumed without handling, so that no
     * fallback opens it either.
     */
    private fun launchRegisteredDeepLink(deepLink: ParkedDeepLink, router: Router): Boolean {
        val handler = deepLinkHandlerRegistry.find(deepLink.uri) ?: return false

        if (deepLink.source == DeeplinkSource.Push && !handler.isOpenableFromPush) {
            TangemLogger.i(
                """
                    Deep link is not openable from push
                    |- Received URI: ${deepLink.uri}
                """.trimIndent(),
            )
            return true
        }

        handler.handle(
            request = DeepLinkRequest(
                uri = deepLink.uri,
                queryParams = getQueryParams(deepLink.uri),
                scope = deepLink.scope,
                isFromOnNewIntent = deepLink.isFromOnNewIntent,
                router = router,
            ),
        )

        return true
    }

    /**
     * A push-supplied URI that matches no in-app route can still be a marketing web page — the original purpose
     * of the payload's `link` key. Open it only when [ExternalUrlValidator] trusts the host, or when it is an
     * AppsFlyer OneLink domain: those are app links the app itself captures, and the round-trip through
     * `ACTION_VIEW` is what lets the AppsFlyer SDK resolve them in
     * [MainActivity.onNewIntent][com.tangem.tap.MainActivity.onNewIntent].
     */
    private fun openWebLinkFromPush(deeplinkUri: Uri) {
        val url = deeplinkUri.toString()
        val host = deeplinkUri.host
        // Tangem links never carry user info, while parsers disagree on where it ends: an unpatched Uri reads
        // `https://evil.com\@tangem.com` as host tangem.com, a browser as evil.com
        val isTrustedWebLink = deeplinkUri.scheme == DeepLinkScheme.Https.scheme &&
            deeplinkUri.userInfo == null &&
            (ExternalUrlValidator.isHostTrusted(host) || host?.lowercase() in APPSFLYER_ONELINK_HOSTS)

        if (isTrustedWebLink) {
            urlOpener.openUrl(url)
        } else {
            TangemLogger.i("Push link is neither routable nor a trusted web link: $deeplinkUri")
        }
    }

    private fun isReadyToRoute(appRoute: AppRoute): Boolean = when (appRoute) {
        AppRoute.Initial,
        is AppRoute.Home,
        is AppRoute.Welcome,
        is AppRoute.PushNotification,
        is AppRoute.Disclaimer,
        is AppRoute.Stories,
        is AppRoute.Onboarding,
        -> false
        else -> true
    }

    private fun getQueryParams(uri: Uri): Map<String, String> {
        // An opaque URI such as `wc:…` has no query: `queryParameterNames` throws for it
        if (uri.isOpaque) return emptyMap()

        val params = mutableMapOf<String, String>()

        uri.queryParameterNames.forEach { paramName ->
            val paramValue = uri.getQueryParameter(paramName)

            if (paramName.uriValidate() && paramValue?.uriValidate() == true) {
                params[paramName] = paramValue
            }
        }

        return params
    }

    private companion object {

        /**
         * AppsFlyer OneLink domains, mirroring the `autoVerify` App Link filters in the manifest. They are not in
         * [ExternalUrlValidator]'s trusted hosts because they serve attribution redirects rather than content.
         */
        val APPSFLYER_ONELINK_HOSTS = setOf("tangem.onelink.me", "join.tangem.com")
    }
}