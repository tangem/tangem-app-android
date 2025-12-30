package com.tangem.core.analytics.models

/**
 * Marker interface for AppsFlyer events
 * Only events implementing this interface will be sent to AppsFlyer
 */
interface AppsFlyerOnlyEvent

/**
 * Marker interface for AppsFlyer included events
 * Events implementing this interface will be sent to AppsFlyer along with other analytics handlers
 */
interface AppsFlyerIncludedEvent {
    val appsFlyerReplacedEvent: String
}