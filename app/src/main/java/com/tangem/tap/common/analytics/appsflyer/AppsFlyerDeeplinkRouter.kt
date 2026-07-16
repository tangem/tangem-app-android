package com.tangem.tap.common.analytics.appsflyer

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.datasource.local.appsflyer.AppsFlyerStore
import com.tangem.domain.appsflyer.AppsFlyerDeeplink
import com.tangem.domain.appsflyer.usecase.ClearAppsFlyerDeeplinkUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
) {

    fun observe(scope: CoroutineScope, currentRoute: Flow<AppRoute?>) {
        combine(
            appsFlyerStore.observeNavigationDeeplink(),
            currentRoute.distinctUntilChanged(),
        ) { deepLinkValue, route ->
            if (deepLinkValue != null && route != null) deepLinkValue to route else null
        }
            .filterNotNull()
            .onEach { (deepLinkValue, route) -> onDeeplinkPending(deepLinkValue, route) }
            .launchIn(scope)
    }

    private suspend fun onDeeplinkPending(deepLinkValue: String, currentRoute: AppRoute) {
        when (AppsFlyerDeeplink.from(deepLinkValue)) {
            AppsFlyerDeeplink.TangemPayMobileOnboarding -> routeTangemPayOnboarding(currentRoute)
            AppsFlyerDeeplink.Referral -> routeReferral(currentRoute)
            null -> TangemLogger.i("Ignoring unknown AppsFlyer deep link value: $deepLinkValue")
        }
    }

    private suspend fun routeTangemPayOnboarding(currentRoute: AppRoute) {
        // Not on an idle entry screen yet: keep the deep link pending, re-evaluate on next route change.
        if (!isIdleEntryPoint(currentRoute)) return

        if (userWalletsListRepository.userWalletsSync().isNotEmpty()) {
            TangemLogger.i("[TangemPay][HWO] Routing AppsFlyer deep link to Tangem Pay onboarding")
            // Authorized: push onto the wallet screen so Back returns to it.
            appRouter.push(AppRoute.TangemPayOnboarding(AppRoute.TangemPayOnboarding.Mode.MobileOnboardingDeeplink))
        } else {
            TangemLogger.i("[TangemPay][HWO] Routing AppsFlyer deep link to hot wallet onboarding")
            // Not authorized: open onboarding as the root, skipping Home/stories (original cold-start flow).
            appRouter.replaceAll(AppRoute.TangemPayHotWalletOnboarding)
        }
        // One-shot: consume the deep link once routed so the user isn't forced back here on relaunch.
        clearAppsFlyerDeeplinkUseCase()
    }

    private suspend fun routeReferral(currentRoute: AppRoute) {
        // Referral targets fresh installs: from an idle entry screen, go straight to hot wallet creation
        // (skips stories). The deep link is NOT cleared here — it stays as referral attribution (read by
        // IsReferralInstallUseCase, cleared on wallet creation); replaceAll keeps it off the back stack.
        if (!isIdleEntryPoint(currentRoute)) return
        if (userWalletsListRepository.userWalletsSync().isNotEmpty()) return

        TangemLogger.i("[Referral] Routing AppsFlyer referral deep link to hot wallet creation")
        appRouter.replaceAll(AppRoute.CreateWalletStart(mode = AppRoute.CreateWalletStart.Mode.HotWallet))
    }
}

// Route only from idle entry screens so an in-progress flow (scan, KYC, onboarding…) isn't interrupted.
internal fun isIdleEntryPoint(currentRoute: AppRoute?): Boolean =
    currentRoute is AppRoute.Home || currentRoute is AppRoute.Stories || currentRoute is AppRoute.Wallet