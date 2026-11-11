package com.tangem.tap.routing.utils

import android.net.Uri
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.DeepLinkRoute
import com.tangem.common.routing.DeepLinkScheme
import com.tangem.common.routing.deeplink.DeepLinkHandlerRegistry
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.common.routing.deeplink.DeeplinkConst
import com.tangem.common.uri.ExternalUrlValidator
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.data.card.sdk.CardSdkProvider
import com.tangem.domain.marketing.MarketingFeatureToggles
import com.tangem.domain.marketing.MarketingUidBindingRepository
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.extensions.uriValidate
import com.tangem.utils.logging.TangemLogger
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@ActivityScoped
internal class DeepLinkFactory @Inject constructor(
    private val cardSdkProvider: CardSdkProvider,
    private val urlOpener: UrlOpener,
    private val marketingFeatureToggles: MarketingFeatureToggles,
    private val marketingUidBindingRepository: MarketingUidBindingRepository,
    private val appCoroutineScope: AppCoroutineScope,
    private val deepLinkHandlerRegistry: DeepLinkHandlerRegistry,
) {
    private val permittedAppRoute = MutableStateFlow(false)

    private var parkedDeeplink: ParkedDeeplink? = null
    private val deepLinkHandlerJobHolder = JobHolder()

    /**
     * Handle [deeplinkUri]: park it until the app is ready to route, then dispatch it to the matching handler.
     *
     * [source] decides the outcome when no handler matches — see [DeeplinkSource]. It defaults to
     * [DeeplinkSource.External], the conservative option, which drops an unroutable URI.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun handleDeeplink(
        deeplinkUri: Uri,
        coroutineScope: CoroutineScope,
        isFromOnNewIntent: Boolean,
        source: DeeplinkSource = DeeplinkSource.External,
    ) {
        if (source == DeeplinkSource.External && deeplinkUri.isTangemPayMainLink()) {
            TangemLogger.i("Tangem Pay main deep link is openable only from push")
            return
        }

        parkedDeeplink = ParkedDeeplink(uri = deeplinkUri, source = source)

        TangemLogger.i(
            """
                Received deep link intent
                |- Received URI: $deeplinkUri
                |- Source: $source
            """.trimIndent(),
        )
        combine(
            permittedAppRoute,
            cardSdkProvider.sdk.uiVisibility(),
        ) { isRoutePermitted, isCardSdkVisible ->
            isRoutePermitted to isCardSdkVisible
        }.transformLatest<Pair<Boolean, Boolean>, Unit> { (isRoutePermitted, isCardSdkVisible) ->
            if (isRoutePermitted && !isCardSdkVisible) {
                parkedDeeplink?.let { (uri, source) ->
                    val isRouted = launchRegisteredDeepLink(
                        deeplinkUri = uri,
                        coroutineScope = coroutineScope,
                        isFromOnNewIntent = isFromOnNewIntent,
                        source = source,
                    )

                    if (!isRouted && source == DeeplinkSource.Push) {
                        openWebLinkFromPush(uri)
                    }
                }
                parkedDeeplink = null
            }
        }
            .launchIn(coroutineScope)
            .saveIn(deepLinkHandlerJobHolder)
    }

    fun bindMarketingUid(deeplinkUri: Uri) {
        // `wc:` and other opaque links have no query: getQueryParameters throws for them
        if (!marketingFeatureToggles.isUidBindingEnabled || deeplinkUri.isOpaque) return

        val uid = deeplinkUri.getQueryParameters(DeeplinkConst.UID_KEY).firstOrNull(String::isNotEmpty) ?: return
        if (deepLinkHandlerRegistry.find(deeplinkUri) == null) return

        appCoroutineScope.launch { marketingUidBindingRepository.bind(uid) }
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

    /**
     * Check if app is ready to handle deeplink
     */
    fun checkRoutingReadiness(appRoute: AppRoute) {
        permittedAppRoute.value = when (appRoute) {
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
    }

    /**
     * Dispatches [deeplinkUri] to the handler a feature contributed for it. Returns `false` when no feature claims it.
     * A push deep link to a handler that is not openable from push is consumed without handling, so that no
     * fallback opens it either.
     */
    private fun launchRegisteredDeepLink(
        deeplinkUri: Uri,
        coroutineScope: CoroutineScope,
        isFromOnNewIntent: Boolean,
        source: DeeplinkSource,
    ): Boolean {
        val handler = deepLinkHandlerRegistry.find(deeplinkUri) ?: return false

        if (source == DeeplinkSource.Push && !handler.isOpenableFromPush) {
            TangemLogger.i(
                """
                    Deep link is not openable from push
                    |- Received URI: $deeplinkUri
                """.trimIndent(),
            )
            return true
        }

        handler.handle(
            request = DeepLinkRequest(
                uri = deeplinkUri,
                queryParams = getQueryParams(deeplinkUri),
                scope = coroutineScope,
                isFromOnNewIntent = isFromOnNewIntent,
            ),
        )

        return true
    }

    /**
     * Whole-segment path match: `/news` and `/news/1-slug` match `/news`, `/newsletter` does not.
     *
     * A plain `startsWith` would claim `/pay-application` and `/newsletter` as routed, and the handler would then
     * silently do nothing — suppressing the web fallback and turning the tap into a no-op.
     */
    private fun String.matchesPathSegment(segment: String): Boolean = this == segment || startsWith("$segment/")

    private fun Uri.isTangemPayMainLink(): Boolean = when (scheme) {
        DeepLinkScheme.Tangem.scheme -> host == DeepLinkRoute.PayAppMain.host
        DeepLinkScheme.Https.scheme -> host == DeepLinkRoute.PayApp.host && isPayAppMainPath()
        else -> false
    }

    private fun Uri.isPayAppMainPath(): Boolean {
        return path.orEmpty().matchesPathSegment(PAY_APP_MAIN_PATH) ||
            pathSegments.firstOrNull() == PAY_APP_MAIN_SEGMENT
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

    /** A deeplink waiting for the app to become routable, and where it came from. */
    private data class ParkedDeeplink(val uri: Uri, val source: DeeplinkSource)

    private companion object {

        /**
         * AppsFlyer OneLink domains, mirroring the `autoVerify` App Link filters in the manifest. They are not in
         * [ExternalUrlValidator]'s trusted hosts because they serve attribution redirects rather than content.
         */
        val APPSFLYER_ONELINK_HOSTS = setOf("tangem.onelink.me", "join.tangem.com")

        const val PAY_APP_MAIN_SEGMENT = "pay-app-main"
        const val PAY_APP_MAIN_PATH = "/$PAY_APP_MAIN_SEGMENT"
    }
}