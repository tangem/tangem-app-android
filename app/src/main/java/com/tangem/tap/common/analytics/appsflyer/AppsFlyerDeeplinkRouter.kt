package com.tangem.tap.common.analytics.appsflyer

import androidx.core.net.toUri
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.entity.InitScreenLaunchMode
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.deeplink.DeepLinkRequest
import com.tangem.domain.appsflyer.AppsFlyerDeeplink
import com.tangem.domain.appsflyer.AppsFlyerDeeplinkTarget
import com.tangem.domain.appsflyer.repository.AppsFlyerRepository
import com.tangem.domain.appsflyer.usecase.ClearAppsFlyerDeeplinkUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.features.tangempay.deeplink.TangemPayOnboardDeepLinkHandler
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reactively routes AppsFlyer deep links persisted by [AppsFlyerReferralParamsHandler].
 *
 * To add a deep link: add it to [AppsFlyerDeeplinkTarget] and a branch in [onDeeplinkPending]
 * (the `when` is exhaustive).
 */
@Singleton
class AppsFlyerDeeplinkRouter @Inject constructor(
    private val appsFlyerRepository: AppsFlyerRepository,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val clearAppsFlyerDeeplinkUseCase: ClearAppsFlyerDeeplinkUseCase,
    private val tangemPayOnboardDeepLinkHandler: TangemPayOnboardDeepLinkHandler,
) {

    fun observe(scope: CoroutineScope, currentRoute: Flow<AppRoute?>, router: Router) {
        val dispatchedUri = AtomicReference<String?>(null)

        combine(
            appsFlyerRepository.observeNavigationDeeplink(),
            currentRoute.distinctUntilChanged(),
        ) { deepLinkValue, route -> deepLinkValue to route }
            .onEach { (deepLinkValue, route) ->
                if (deepLinkValue == null) {
                    dispatchedUri.set(null)
                    return@onEach
                }
                if (route != null) {
                    onDeeplinkPending(
                        deepLinkValue = deepLinkValue,
                        currentRoute = route,
                        dispatchedUri = dispatchedUri,
                        scope = scope,
                        router = router,
                    )
                }
            }
            .launchIn(scope)
    }

    private suspend fun onDeeplinkPending(
        deepLinkValue: String,
        currentRoute: AppRoute,
        dispatchedUri: AtomicReference<String?>,
        scope: CoroutineScope,
        router: Router,
    ) {
        when (val target = AppsFlyerDeeplinkTarget.from(deepLinkValue)) {
            is AppsFlyerDeeplinkTarget.Known -> when (target.deeplink) {
                AppsFlyerDeeplink.TangemPayMobileOnboarding -> routeTangemPayOnboarding(currentRoute, router)
                AppsFlyerDeeplink.Referral -> routeReferral(currentRoute, router)
            }
            is AppsFlyerDeeplinkTarget.TangemPayCampaign -> routeTangemPayCampaign(target.key, currentRoute, router)
            is AppsFlyerDeeplinkTarget.Direct -> routeDeferred(
                uri = target.uri,
                currentRoute = currentRoute,
                dispatchedUri = dispatchedUri,
                scope = scope,
                router = router,
            )
            null -> TangemLogger.i("Ignoring unknown AppsFlyer deep link value: $deepLinkValue")
        }
    }

    private suspend fun routeDeferred(
        uri: String,
        currentRoute: AppRoute,
        dispatchedUri: AtomicReference<String?>,
        scope: CoroutineScope,
        router: Router,
    ) {
        if (!isSupportedDeferredDeeplink(uri)) {
            TangemLogger.i("[Deferred] Ignoring unsupported AppsFlyer deep link URI: $uri")
            return
        }
        if (!isIdleEntryPoint(currentRoute)) return

        if (dispatchedUri.getAndSet(uri) != uri) {
            TangemLogger.i("[Deferred] Dispatching AppsFlyer deep link URI: $uri")
            tangemPayOnboardDeepLinkHandler.handle(
                request = DeepLinkRequest(
                    uri = uri.toUri(),
                    queryParams = emptyMap(),
                    scope = scope,
                    isFromOnNewIntent = false,
                    router = router,
                ),
            )
        }
        clearAppsFlyerDeeplinkUseCase()
    }

    private suspend fun routeTangemPayOnboarding(currentRoute: AppRoute, router: Router) {
        // A flow is in progress: keep the deep link pending, re-evaluate on next route change.
        if (isNavigationBlocked(currentRoute)) return

        if (userWalletsListRepository.userWalletsSync().isEmpty()) {
            routeTangemPayOnboardingOnFreshInstall(currentRoute, router)
            return
        }

        TangemLogger.i("[TangemPay][HWO] Routing AppsFlyer deep link to Tangem Pay onboarding")
        // Authorized: push onto the wallet screen so Back returns to it.
        router.push(AppRoute.TangemPayOnboarding(AppRoute.TangemPayOnboarding.Mode.MobileOnboardingDeeplink))
        // One-shot: consume the deep link once routed so the user isn't forced back here on relaunch.
        clearAppsFlyerDeeplinkUseCase()
    }

    /**
     * No wallet yet: the hot wallet onboarding creates one as part of the Tangem Pay flow, so it replaces the
     * start screen instead of waiting for a tap on it.
     */
    private suspend fun routeTangemPayOnboardingOnFreshInstall(currentRoute: AppRoute, router: Router) {
        // Also the re-entry guard: the route this method installs is not an install entry, so the re-emission
        // that `replaceAll` triggers cannot route a second time before the clear has landed.
        if (!isFreshInstallEntry(currentRoute)) return

        // An NFC launch already owns the screen; DeepLinkFactory defers to the card SDK for the same reason.
        if (currentRoute is AppRoute.Home && currentRoute.launchMode == InitScreenLaunchMode.WithCardScan) {
            TangemLogger.i("[TangemPay][HWO] Card scan is in progress, keeping the deep link")
            return
        }

        TangemLogger.i("[TangemPay][HWO] Routing AppsFlyer deep link as the root")
        // Consumed before navigating, not after: on a device without hot wallet support the onboarding falls back
        // to Home, and a link still pending by then would route again from there, forever.
        clearAppsFlyerDeeplinkUseCase()
        // Root, not push: that same fallback uses `replaceCurrent`, which would otherwise stack a second Home.
        router.replaceAll(AppRoute.TangemPayHotWalletOnboarding)
    }

    private suspend fun routeTangemPayCampaign(campaignKey: String, currentRoute: AppRoute, router: Router) {
        if (userWalletsListRepository.userWalletsSync().isNotEmpty()) {
            if (isNavigationBlocked(currentRoute)) return
            TangemLogger.i("[TangemPay][Campaign] Routing campaign deep link over the current screen")
            router.push(AppRoute.TangemPayCampaign(campaignKey))
        } else {
            if (!isFreshInstallEntry(currentRoute)) return
            TangemLogger.i("[TangemPay][Campaign] Routing campaign deep link as the root")
            router.replaceAll(AppRoute.TangemPayCampaign(campaignKey))
        }
    }

    private suspend fun routeReferral(currentRoute: AppRoute, router: Router) {
        // Referral targets fresh installs: go straight to hot wallet creation. The deep link is NOT cleared
        // here — ClearReferralInstallUseCase clears it on wallet creation; replaceAll keeps it off the back stack.
        if (!isFreshInstallEntry(currentRoute)) return
        if (userWalletsListRepository.userWalletsSync().isNotEmpty()) return

        TangemLogger.i("[Referral] Routing AppsFlyer referral deep link to hot wallet creation")
        router.replaceAll(AppRoute.CreateWalletStart)
    }
}

internal fun isFreshInstallEntry(currentRoute: AppRoute?): Boolean = when (currentRoute) {
    // A Home reached by forgetting the last wallet is a return, not an install entry: a link left pending by an
    // earlier session must not hijack it.
    is AppRoute.Home -> currentRoute.entryReason == AppRoute.Home.EntryReason.Launch
    else -> false
}

// Mirrors DeepLinkFactory.checkRoutingReadiness (minus Home and Stories, which these links may route from) plus
// the Tangem Pay flows these links open, so a pending link never stacks a second one on top of them.
internal fun isNavigationBlocked(currentRoute: AppRoute?): Boolean = when (currentRoute) {
    null,
    is AppRoute.Initial,
    is AppRoute.Welcome,
    is AppRoute.PushNotification,
    is AppRoute.Disclaimer,
    is AppRoute.Onboarding,
    is AppRoute.TangemPayCampaign,
    is AppRoute.TangemPayOnboarding,
    is AppRoute.TangemPayHotWalletOnboarding,
    is AppRoute.Kyc,
    -> true
    else -> false
}

// Route only from idle entry screens so an in-progress flow (scan, KYC, onboarding…) isn't interrupted.
internal fun isIdleEntryPoint(currentRoute: AppRoute?): Boolean =
    currentRoute is AppRoute.Home || currentRoute is AppRoute.Stories || currentRoute is AppRoute.Wallet

private const val TANGEM_PAY_ONBOARD_DEEPLINK = TangemPayOnboardDeepLinkHandler.TANGEM_DEEPLINK_KEY

internal fun isSupportedDeferredDeeplink(uri: String): Boolean = uri == TANGEM_PAY_ONBOARD_DEEPLINK ||
    uri.startsWith("$TANGEM_PAY_ONBOARD_DEEPLINK?") ||
    uri.startsWith("$TANGEM_PAY_ONBOARD_DEEPLINK/")