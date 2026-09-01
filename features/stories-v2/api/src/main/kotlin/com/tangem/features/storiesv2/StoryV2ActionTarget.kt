package com.tangem.features.storiesv2

/**
 * What an action button is wired to. The player never navigates on its own — it hands the target back to the host,
 * which owns the routing table. A `close` target is the exception: the player resolves it itself.
 */
sealed interface StoryV2ActionTarget {

    /** A screen key from the app's allowlist, e.g. `hardware_wallet`. */
    data class Screen(val key: String) : StoryV2ActionTarget

    /** An HTTPS url from the allowlist, opened in the embedded webview. */
    data class Web(val url: String) : StoryV2ActionTarget

    /** A `tangem://` deep link registered in the app. */
    data class Deeplink(val uri: String) : StoryV2ActionTarget
}