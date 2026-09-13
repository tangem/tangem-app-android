package com.tangem.core.remote.header

import com.tangem.core.remote.header.RequestHeader.CacheControlHeader.checkHeaderValueOrEmpty
import com.tangem.utils.ProviderSuspend
import com.tangem.utils.info.AppInfoProvider
import java.util.TimeZone

/**
 * Presentation of request header
 *
 * Open (not sealed) so streams can declare auth-coupled headers in their own modules without pulling their
 * auth providers up here — this module holds only the dependency-free headers.
 *
 * @param pairs header name and header value pairs
 */
open class RequestHeader(vararg pairs: Pair<String, ProviderSuspend<String>>) {

    /** Header list */
    val values: Map<String, ProviderSuspend<String>> = pairs.toMap()

    data object CacheControlHeader : RequestHeader("Cache-Control" to ProviderSuspend { "max-age=600" })

    class AppVersionPlatformHeaders(
        appInfoProvider: AppInfoProvider,
    ) : RequestHeader(
        "system_version" to ProviderSuspend { appInfoProvider.osVersion },
        "version" to ProviderSuspend { appInfoProvider.appVersion },
        "platform" to ProviderSuspend { "android" },
        "language" to ProviderSuspend { appInfoProvider.language.checkHeaderValueOrEmpty() },
        "timezone" to ProviderSuspend {
            val timeZone = TimeZone.getDefault()
            // TimeZone.getDisplayName may throw AssertionError on some devices due to an Android ICU bug
            // ("No NameTypeIndex match for SHORT_STANDARD"), so fall back to the timezone id.
            val displayName = try {
                timeZone.getDisplayName(false, TimeZone.SHORT)
            } catch (_: AssertionError) {
                timeZone.id
            }
            displayName.checkHeaderValueOrEmpty()
        },
        "device" to ProviderSuspend { appInfoProvider.device.checkHeaderValueOrEmpty() },
    )

    /**
     * Use it to avoid crash in okhttp headers
     */
    fun String.checkHeaderValueOrEmpty(): String {
        for (i in this.indices) {
            val c = this[i]
            val isChar = c == '\t' || c in '\u0020'..'\u007e'
            if (!isChar) {
                return ""
            }
        }
        return this
    }
}