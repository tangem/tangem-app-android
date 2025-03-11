package com.tangem.core.analytics.models.event

import com.tangem.core.analytics.models.AnalyticsEvent

/**
 * Tech analytics event
 *
 * @param event  event name
 * @param params params
 */
sealed class TechAnalyticsEvent(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category = "Tech", event = event, params = params) {

    class WindowObscured(state: ObscuredState) : TechAnalyticsEvent(
        event = "Window Obscured",
        params = mapOf("State" to state.name),
    ) {

        enum class ObscuredState {
            PARTIALLY,
            FULLY,
        }
    }

    class KeyboardIdentifier(id: String, packageName: String?, isTrusted: Boolean) : TechAnalyticsEvent(
        event = "Keyboard Identifier",
        params = buildMap {
            put("Id", id)
            packageName?.let {
                put("Package", it)
                put("GPUrl", "https://play.google.com/store/apps/details?id=$packageName")
            }
            put("isTrusted", isTrusted.toString())
        },
    )
}