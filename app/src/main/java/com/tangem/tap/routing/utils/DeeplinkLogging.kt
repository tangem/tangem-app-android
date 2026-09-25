package com.tangem.tap.routing.utils

import android.net.Uri

/**
 * Deep-link URIs are written to the persisted app log (the one attached to support requests). Query values can
 * carry a sell deposit address and `request_id`, survey tokens or referral codes, so only the shape is logged.
 */
internal fun Uri?.redactedForLog(): String {
    if (this == null) return "null"
    // Opaque URIs (e.g. "wc:<topic>@2?symKey=…") carry their secret in the scheme-specific part.
    if (isOpaque) return "$scheme:$REDACTED_VALUE"
    val redacted = buildUpon().clearQuery()
    queryParameterNames.forEach { name -> redacted.appendQueryParameter(name, REDACTED_VALUE) }
    return redacted.build().toString()
}

private const val REDACTED_VALUE = "***"
