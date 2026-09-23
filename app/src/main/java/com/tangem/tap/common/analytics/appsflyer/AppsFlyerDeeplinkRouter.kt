package com.tangem.tap.common.analytics.appsflyer

import androidx.core.net.toUri
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.DeepLinkRoute
import com.tangem.common.routing.DeepLinkScheme
import com.tangem.datasource.local.appsflyer.AppsFlyerStore
import com.tangem.domain.appsflyer.AppsFlyerDeeplink
import com.tangem.domain.appsflyer.AppsFlyerDeeplinkTarget
import com.tangem.domain.appsflyer.usecase.ClearAppsFlyerDeeplinkUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.features.tangempay.deeplink.OnboardVisaDeepLinkHandler
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
 * To add a deep link: add it to [AppsFlyerDeeplink] and a branch in [onDeeplinkPending] (the `when` is exhaustive).
 */
@Singleton
class AppsFlyerDeeplinkRouter @Inject constructor(
    private val appsFlyerStore: AppsFlyerStore,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val clearAppsFlyerDeeplinkUseCase: ClearAppsFlyerDeeplinkUseCase,
    private val appRouter: AppRouter,
    private val onboardVisaDeepLink: OnboardVisaDeepLinkHandler.Factory,
) {

    fun observe(scope: CoroutineScope, currentRoute: Flow<AppRoute?>) {
        val dispatchedUri = AtomicReference<String?>(null)

        combine(
            appsFlyerStore.observeNavigationDeeplink(),
            currentRoute.distinctUntilChanged(),
        ) { deepLinkValue, route -> deepLinkValue to route }
            .onEach { (deepLinkValue, route) ->
                if (deepLinkValue == null) {
                    dispatchedUri.set(null)
                    return@onEach
                }
                if (route != null) onDeeplinkPending(deepLinkValue, route, dispatchedUri)
            }
            .launchIn(scope)
    }

    private suspend fun onDeeplinkPending(
        deepLinkValue: String,
        currentRoute: AppRoute,
        dispatchedUri: AtomicReference<String?>,
    ) {
        when (val target = AppsFlyerDeeplinkTarget.from(deepLinkValue)) {
            is AppsFlyerDeeplinkTarget.Known -> when (target.deeplink) {
                AppsFlyerDeeplink.TangemPayMobileOnboarding -> routeTangemPayOnboarding(currentRoute)
                AppsFlyerDeeplink.Referral -> routeReferral(currentRoute)
            }
            is AppsFlyerDeeplinkTarget.Direct -> routeDeferred(target.uri, currentRoute, dispatchedUri)
            null -> TangemLogger.i("Ignoring unknown AppsFlyer deep link value: $deepLinkValue")
        }
    }

    private suspend fun routeDeferred(uri: String, currentRoute: AppRoute, dispatchedUri: AtomicReference<String?>) {
        if (!isSupportedDeferredDeeplink(uri)) {
            TangemLogger.i("[Deferred] Ignoring unsupported AppsFlyer deep link URI: $uri")
            return
        }
        if (!isIdleEntryPoint(currentRoute)) return

        if (dispatchedUri.getAndSet(uri) != uri) {
            TangemLogger.i("[Deferred] Dispatching AppsFlyer deep link URI: $uri")
            onboardVisaDeepLink.create(uri.toUri())
        }
        clearAppsFlyerDeeplinkUseCase()
    }

    private suspend fun routeTangemPayOnboarding(currentRoute: AppRoute) {
        // Not on an idle entry screen yet: keep the deep link pending, re-evaluate on next route change.
        if (!isIdleEntryPoint(currentRoute)) return

        // No wallet yet: the user is on the intro stories, which this deep link must not skip. It stays pending
        // and Home consumes it on "Get started", right after the stories (see HomeModel.onGetStartedClick).
        if (userWalletsListRepository.userWalletsSync().isEmpty()) {
            TangemLogger.i("[TangemPay][HWO] No wallets yet, keeping the deep link until the stories are over")
            return
        }

        TangemLogger.i("[TangemPay][HWO] Routing AppsFlyer deep link to Tangem Pay onboarding")
        // Authorized: push onto the wallet screen so Back returns to it.
        appRouter.push(AppRoute.TangemPayOnboarding(AppRoute.TangemPayOnboarding.Mode.MobileOnboardingDeeplink))
        // One-shot: consume the deep link once routed so the user isn't forced back here on relaunch.
        clearAppsFlyerDeeplinkUseCase()
    }

    private suspend fun routeReferral(currentRoute: AppRoute) {
        // Referral targets fresh installs: from an idle entry screen, go straight to hot wallet creation
        // (skips stories). The deep link is NOT cleared here — it stays as referral attribution (read by
        // GetAppsFlyerDeeplinkUseCase, cleared on wallet creation); replaceAll keeps it off the back stack.
        if (!isIdleEntryPoint(currentRoute)) return
        if (userWalletsListRepository.userWalletsSync().isNotEmpty()) return

        TangemLogger.i("[Referral] Routing AppsFlyer referral deep link to hot wallet creation")
        appRouter.replaceAll(AppRoute.CreateWalletStart(mode = AppRoute.CreateWalletStart.Mode.HotWallet))
    }
}

// Route only from idle entry screens so an in-progress flow (scan, KYC, onboarding…) isn't interrupted.
internal fun isIdleEntryPoint(currentRoute: AppRoute?): Boolean =
    currentRoute is AppRoute.Home || currentRoute is AppRoute.Stories || currentRoute is AppRoute.Wallet

private val ONBOARD_VISA_DEEPLINK = "${DeepLinkScheme.Tangem.scheme}://${DeepLinkRoute.OnboardVisa.host}"

internal fun isSupportedDeferredDeeplink(uri: String): Boolean = uri == ONBOARD_VISA_DEEPLINK ||
    uri.startsWith("$ONBOARD_VISA_DEEPLINK?") ||
    uri.startsWith("$ONBOARD_VISA_DEEPLINK/")