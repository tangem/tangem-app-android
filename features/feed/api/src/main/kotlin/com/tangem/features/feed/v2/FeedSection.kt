package com.tangem.features.feed.v2

import com.tangem.features.feed.nav.FeedTabId

/**
 * Section of the feed hub that can be addressed from outside the app — the `section` parameter of the
 * `tangem://discover` deeplink.
 *
 * The wire names are deliberately not the tab ids (`assets` opens [FeedTabId.RealAssets]), and not every
 * tab is addressable, so this mapping is the contract — never build a [FeedTabId] from a raw parameter.
 */
enum class FeedSection(val value: String, val tabId: FeedTabId) {

    Crypto(value = "crypto", tabId = FeedTabId.Crypto),
    Assets(value = "assets", tabId = FeedTabId.RealAssets),
    Earn(value = "earn", tabId = FeedTabId.Earn),
    ;

    companion object {

        /** `null` for an unknown or absent section — the hub then opens on its default tab. */
        fun parse(value: String?): FeedSection? = entries.firstOrNull { it.value == value }
    }
}