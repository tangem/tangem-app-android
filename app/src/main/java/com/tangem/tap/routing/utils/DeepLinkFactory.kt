package com.tangem.tap.routing.utils

import android.net.Uri
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.DeepLinkRoute
import com.tangem.common.routing.DeepLinkScheme
import com.tangem.common.uri.ExternalUrlValidator
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.data.card.sdk.CardSdkProvider
import com.tangem.feature.referral.api.deeplink.ReferralDeepLinkHandler
import com.tangem.features.feed.entry.deeplink.EarnDeepLinkHandler
import com.tangem.features.feed.entry.deeplink.MarketsDeepLinkHandler
import com.tangem.features.feed.entry.deeplink.MarketsTokenDetailDeepLinkHandler
import com.tangem.features.feed.entry.deeplink.MarketsTokenExchangesDeepLinkHandler
import com.tangem.features.feed.entry.deeplink.NewsDeepLinkHandler
import com.tangem.features.feed.entry.deeplink.NewsDetailsDeepLinkHandler
import com.tangem.features.feed.entry.deeplink.YieldDeepLinkHandler
import com.tangem.features.onramp.deeplink.BuyDeepLinkHandler
import com.tangem.features.onramp.deeplink.OnrampDeepLinkHandler
import com.tangem.features.onramp.deeplink.SellDeepLinkHandler
import com.tangem.features.onramp.deeplink.SwapDeepLinkHandler
import com.tangem.features.promobanners.api.deeplink.CampaignsDeepLinkHandler
import com.tangem.features.send.api.deeplink.SellRedirectDeepLinkHandler
import com.tangem.features.staking.api.deeplink.StakingDeepLinkHandler
import com.tangem.features.survey.deeplink.SurveyDeepLinkHandler
import com.tangem.features.tangempay.deeplink.OnboardVisaDeepLinkHandler
import com.tangem.features.virtualaccount.onboarding.deeplink.OnboardVirtualAccountsDeepLinkHandler
import com.tangem.features.tangempay.deeplink.TangemPayMainDeepLinkHandler
import com.tangem.features.tokendetails.deeplink.TokenDetailsDeepLinkHandler
import com.tangem.features.wallet.deeplink.PromoDeeplinkHandler
import com.tangem.features.wallet.deeplink.WalletDeepLinkHandler
import com.tangem.features.walletconnect.components.deeplink.WalletConnectDeepLinkHandler
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
import javax.inject.Inject

@Suppress("LongParameterList")
@ActivityScoped
internal class DeepLinkFactory @Inject constructor(
    private val cardSdkProvider: CardSdkProvider,
    private val onrampDeepLink: OnrampDeepLinkHandler.Factory,
    private val sellRedirectDeepLink: SellRedirectDeepLinkHandler.Factory,
    private val referralDeepLink: ReferralDeepLinkHandler.Factory,
    private val walletConnectDeepLink: WalletConnectDeepLinkHandler.Factory,
    private val walletDeepLink: WalletDeepLinkHandler.Factory,
    private val tokenDetailsDeepLink: TokenDetailsDeepLinkHandler.Factory,
    private val stakingDeepLink: StakingDeepLinkHandler.Factory,
    private val marketsDeepLink: MarketsDeepLinkHandler.Factory,
    private val marketsTokenDetailDeepLink: MarketsTokenDetailDeepLinkHandler.Factory,
    private val buyDeepLink: BuyDeepLinkHandler.Factory,
    private val sellDeepLink: SellDeepLinkHandler.Factory,
    private val swapDeepLink: SwapDeepLinkHandler.Factory,
    private val promoDeepLink: PromoDeeplinkHandler.Factory,
    private val onboardVisaDeepLink: OnboardVisaDeepLinkHandler.Factory,
    private val onboardVirtualAccountsDeepLink: OnboardVirtualAccountsDeepLinkHandler.Factory,
    private val marketsTokenExchangesDeepLink: MarketsTokenExchangesDeepLinkHandler.Factory,
    private val tangemPayMainDeepLink: TangemPayMainDeepLinkHandler.Factory,
    private val newsDetailsDeepLink: NewsDetailsDeepLinkHandler.Factory,
    private val newsDeepLink: NewsDeepLinkHandler.Factory,
    private val earnDeepLink: EarnDeepLinkHandler.Factory,
    private val yieldDeepLink: YieldDeepLinkHandler.Factory,
    private val surveyDeepLink: SurveyDeepLinkHandler.Factory,
    private val promoCampaignsDeepLink: CampaignsDeepLinkHandler.Factory,
    private val urlOpener: UrlOpener,
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
                    val isRouted = launchDeepLink(uri, coroutineScope, isFromOnNewIntent)

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

    /**
     * A push-supplied URI that matches no in-app route can still be a marketing web page — the original purpose
     * of the payload's `link` key. Open it only when [ExternalUrlValidator] trusts the host, or when it is an
     * AppsFlyer OneLink domain: those are app links the app itself captures, and the round-trip through
     * `ACTION_VIEW` is what lets the AppsFlyer SDK resolve them in
     * [MainActivity.onNewIntent][com.tangem.tap.MainActivity.onNewIntent].
     *
     * The host is matched against the already-parsed [Uri] rather than through
     * [ExternalUrlValidator.isUriTrusted], which re-parses with `java.net.URI` and reports a malformed string to
     * Crashlytics — one badly typed campaign URL would otherwise fan out into a report from every device.
     */
    private fun openWebLinkFromPush(deeplinkUri: Uri) {
        val url = deeplinkUri.toString()
        val host = deeplinkUri.host
        val isTrustedWebLink = deeplinkUri.scheme == DeepLinkScheme.Https.scheme &&
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

    /** Returns `true` when a handler took the deeplink, `false` when nothing matched it. */
    private fun launchDeepLink(deeplinkUri: Uri, coroutineScope: CoroutineScope, isFromOnNewIntent: Boolean): Boolean {
        return when (deeplinkUri.scheme) {
            DeepLinkScheme.Https.scheme -> handleHttpDeepLinks(deeplinkUri, coroutineScope)
            DeepLinkScheme.Tangem.scheme -> handleTangemDeepLinks(deeplinkUri, coroutineScope, isFromOnNewIntent)
            DeepLinkScheme.WalletConnect.scheme -> {
                walletConnectDeepLink.create(deeplinkUri)
                true
            }
            else -> {
                TangemLogger.i(
                    """
                        No match found for deep link
                        |- Received URI: $deeplinkUri
                    """.trimIndent(),
                )
                false
            }
        }
    }

    private fun handleHttpDeepLinks(deeplinkUri: Uri, coroutineScope: CoroutineScope): Boolean {
        if (deeplinkUri.host != DeepLinkRoute.PayApp.host) return false

        val path = deeplinkUri.path.orEmpty()

        return when {
            path.matchesPathSegment("/pay-app-main") -> {
                tangemPayMainDeepLink.create(coroutineScope, getQueryParams(deeplinkUri))
                true
            }
            path.matchesPathSegment("/pay-app") -> {
                onboardVisaDeepLink.create(deeplinkUri)
                true
            }
            path.matchesPathSegment("/news") -> {
                // Article-less paths (`/news`, `/news/{category}`) would silently no-op in the details
                // handler, and the web fallback can't open them either: the manifest claims `/news*` as a
                // verified App Link, so ACTION_VIEW would bounce straight back. Show the news list instead.
                if (NewsDetailsDeepLinkHandler.extractArticleIdFromUri(deeplinkUri) != null) {
                    newsDetailsDeepLink.create(coroutineScope, deeplinkUri)
                } else {
                    newsDeepLink.create(getQueryParams(deeplinkUri))
                }
                true
            }
            else -> false
        }
    }

    /**
     * Whole-segment path match: `/news` and `/news/1-slug` match `/news`, `/newsletter` does not.
     *
     * A plain `startsWith` would claim `/pay-application` and `/newsletter` as routed, and the handler would then
     * silently do nothing — suppressing the web fallback and turning the tap into a no-op.
     */
    private fun String.matchesPathSegment(segment: String): Boolean = this == segment || startsWith("$segment/")

    @Suppress("CyclomaticComplexMethod")
    private fun handleTangemDeepLinks(
        deeplinkUri: Uri,
        coroutineScope: CoroutineScope,
        isFromOnNewIntent: Boolean,
    ): Boolean {
        val queryParams = getQueryParams(deeplinkUri)
        when (deeplinkUri.host) {
            DeepLinkRoute.Onramp.host -> onrampDeepLink.create(coroutineScope, queryParams)
            DeepLinkRoute.SellRedirect.host -> sellRedirectDeepLink.create(coroutineScope, queryParams)
            DeepLinkRoute.BuyRedirect.host -> Unit
            DeepLinkRoute.Referral.host -> referralDeepLink.create()
            DeepLinkRoute.Wallet.host -> walletDeepLink.create()
            DeepLinkRoute.TokenDetails.host -> tokenDetailsDeepLink.create(
                coroutineScope = coroutineScope,
                queryParams = queryParams,
                isFromOnNewIntent = isFromOnNewIntent,
            )
            DeepLinkRoute.Staking.host -> stakingDeepLink.create(coroutineScope, queryParams)
            DeepLinkRoute.Markets.host -> marketsDeepLink.create(queryParams)
            DeepLinkRoute.MarketTokenDetail.host -> marketsTokenDetailDeepLink.create(coroutineScope, queryParams)
            DeepLinkRoute.TokenExchanges.host -> marketsTokenExchangesDeepLink.create(coroutineScope, queryParams)
            DeepLinkRoute.Buy.host -> buyDeepLink.create()
            DeepLinkRoute.Sell.host -> sellDeepLink.create()
            DeepLinkRoute.Swap.host -> swapDeepLink.create()
            DeepLinkRoute.WalletConnect.host -> walletConnectDeepLink.create(deeplinkUri)
            DeepLinkRoute.Promo.host -> promoDeepLink.create(coroutineScope, queryParams)
            DeepLinkRoute.OnboardVisa.host -> onboardVisaDeepLink.create(deeplinkUri)
            DeepLinkRoute.OnboardVirtualAccounts.host -> onboardVirtualAccountsDeepLink.create(deeplinkUri)
            DeepLinkRoute.News.host -> newsDeepLink.create(queryParams)
            DeepLinkRoute.Earn.host -> earnDeepLink.create(queryParams)
            DeepLinkRoute.Yield.host -> yieldDeepLink.create(coroutineScope, queryParams)
            DeepLinkRoute.PayAppMain.host -> tangemPayMainDeepLink.create(coroutineScope, queryParams)
            DeepLinkRoute.Survey.host -> surveyDeepLink.create(queryParams)
            DeepLinkRoute.Campaigns.host -> promoCampaignsDeepLink.create(queryParams)
            else -> {
                TangemLogger.i(
                    """
                        No match found for deep link
                        |- Received URI: $deeplinkUri
                        |- With params: $queryParams
                    """.trimIndent(),
                )
                return false
            }
        }

        return true
    }

    private fun getQueryParams(uri: Uri): Map<String, String> {
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
    }
}