package com.tangem.common.routing.deeplink

import dagger.MapKey

/**
 * Map key of a [DeepLinkHandler]: the URI prefix the handler claims, in one of the forms
 * - `scheme://host` — e.g. `tangem://staking`;
 * - `scheme://host/segment[/segment…]` — whole path segments, e.g. `https://tangem.com/joint-account/join`;
 * - `scheme:` — every link of the scheme, e.g. `wc:`.
 *
 * Empty path segments are ignored, so `tangem://staking/` equals `tangem://staking`; percent-encoded characters are
 * decoded like in the received URI, except `%2F`. Query and fragment are not allowed.
 * The most specific key wins, see [DeepLinkHandlerRegistry].
 */
@MapKey
@Retention(AnnotationRetention.RUNTIME)
annotation class DeepLinkKey(val value: String)