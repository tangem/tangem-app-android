package com.tangem.features.feed.v2

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.StateFlow

/**
 * Seam for opening the feed hub from outside it — today the `tangem://shtorka` deeplink.
 *
 * The hub is not a navigation destination: it lives inside the wallet screen, so a caller cannot route
 * to a section of it. It parks a [FeedEntryRequest] here instead, and the hub picks it up when it next
 * exists.
 *
 * A pending *slot* rather than an event, because a request outlives its publisher — it is normally made
 * before the hub exists at all. Explicit consumption is what stops it re-firing later, since the wallet
 * screen leaves composition whenever a screen is pushed over it.
 */
@Stable
interface FeedEntryController {

    /**
     * A request to open the hub, until the host has acted on it. Present even when it names no
     * section — the hub still opens, on its default one.
     */
    val pendingEntry: StateFlow<FeedEntryRequest?>

    fun request(request: FeedEntryRequest)

    /** Called by the feed host once the hub has been opened on the requested section. */
    fun consumeEntry()
}