package com.tangem.common.routing.deeplink

/**
 * Handles deep links of one feature. Bound into the map by [DeepLinkKey]:
 *
 * ```kotlin
 * @Binds @IntoMap @DeepLinkKey("tangem://staking")
 * fun bindStakingDeepLinkHandler(impl: DefaultStakingDeepLinkHandler): DeepLinkHandler
 * ```
 */
fun interface DeepLinkHandler {

    /** `false` — the handler is skipped as if it were not registered. Checked on every deep link. */
    val isEnabled: Boolean
        get() = true

    /** `false` — a deep link from a push notification is dropped instead of being handled. */
    val isOpenableFromPush: Boolean
        get() = true

    fun handle(request: DeepLinkRequest)
}

/** Creates a [DeepLinkHandler] that is enabled while [isEnabled] returns `true`, e.g. behind a feature toggle. */
fun DeepLinkHandler(isEnabled: () -> Boolean, handle: (DeepLinkRequest) -> Unit): DeepLinkHandler {
    val isEnabledProvider = isEnabled
    val handleRequest = handle

    return object : DeepLinkHandler {
        override val isEnabled: Boolean
            get() = isEnabledProvider()

        override fun handle(request: DeepLinkRequest) = handleRequest(request)
    }
}