package com.tangem.features.storiesv2.impl.content.remote

import com.tangem.features.storiesv2.StoryV2Type

/**
 * What this release knows about remote stories. By contract a new slug, placement or deep link host needs a release,
 * so they live in code instead of arriving from the backend.
 */
internal object StoryV2Registry {

    private val remoteSlugs: Map<StoryV2Type, String> = mapOf(
        StoryV2Type.HARDWARE_UPGRADE to "hardware_upgrade",
    )

    /** Requested together in one call, sorted so the saved response can be matched against the next request. */
    val slugs: List<String> = remoteSlugs.values.distinct().sorted()

    val placements: Set<String> = setOf("main_upgrade", "wallet_settings_upgrade")

    /** `tangem://` hosts the app routes. An action pointing anywhere else is hidden rather than shown dead. */
    val deeplinkHosts: Set<String> = setOf("hardware_wallet")

    /** HTTPS hosts an action may open in the webview, straight from the contract's allowlist. */
    val webHosts: Set<String> = setOf(
        "promo.tangem.com",
        "tangem.com",
        "www.tangem.com",
        "buy.tangem.com",
        "app.tangem.com",
        "tangem.surveysparrow.com",
        "feedback.tangem.com",
    )

    val remoteTypes: List<StoryV2Type> = remoteSlugs.keys.toList()

    fun slugOf(type: StoryV2Type): String? = remoteSlugs[type]
}