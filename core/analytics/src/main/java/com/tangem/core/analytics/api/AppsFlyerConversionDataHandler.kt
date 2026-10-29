package com.tangem.core.analytics.api

/**
 * Receives the AppsFlyer install conversion data once the SDK resolves it. Implementations are contributed
 * with `@IntoSet` and called in no particular order.
 */
interface AppsFlyerConversionDataHandler {

    fun handle(conversionData: Map<String?, Any?>)
}