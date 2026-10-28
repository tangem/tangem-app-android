package com.tangem.features.polymarket.impl.common

import androidx.compose.runtime.Immutable
import java.net.URI

@Immutable
internal data class PolymarketResolutionSourceUM(val text: String, val url: String?)

internal fun String?.toResolutionSourceUM(): PolymarketResolutionSourceUM? {
    val source = this?.trim()?.takeIf(String::isNotEmpty) ?: return null
    val webUri = runCatching { URI(source) }.getOrNull()?.takeIf { uri ->
        !uri.host.isNullOrBlank() && (uri.scheme.equals("https", ignoreCase = true) ||
            uri.scheme.equals("http", ignoreCase = true))
    }
    return PolymarketResolutionSourceUM(
        text = webUri?.host?.removePrefix("www.") ?: source,
        url = source.takeIf { webUri != null },
    )
}