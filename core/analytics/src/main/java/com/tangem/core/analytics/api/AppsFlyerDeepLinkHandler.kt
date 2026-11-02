package com.tangem.core.analytics.api

/** Receives the deep links the AppsFlyer SDK resolves through Unified Deep Linking. */
interface AppsFlyerDeepLinkHandler {

    fun handle(deepLink: AppsFlyerDeepLink)
}

/**
 * A deep link resolved by the AppsFlyer SDK.
 *
 * @property isDeferred whether the link was delivered after install rather than by opening the app; `null` when the
 * SDK does not report it
 */
data class AppsFlyerDeepLink(
    val deepLinkValue: String?,
    val afDp: String?,
    val deepLinkSub1: String?,
    val deepLinkSub2: String?,
    val isDeferred: Boolean?,
)