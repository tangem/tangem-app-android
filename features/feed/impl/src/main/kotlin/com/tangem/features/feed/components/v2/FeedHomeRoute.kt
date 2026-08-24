package com.tangem.features.feed.components.v2

import com.tangem.features.feed.nav.FeedRoute
import kotlinx.serialization.Serializable

/**
 * Root of the feed stack: top blocks + tabs. Host-internal — always the bottom of the stack, never
 * pushed and never a navigation target for plug-ins, hence not part of the api contract.
 */
@Serializable
internal data object FeedHomeRoute : FeedRoute