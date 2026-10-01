package com.tangem.common.routing.deeplink

import android.net.Uri
import com.tangem.utils.logging.TangemLogger

/**
 * Finds the [DeepLinkHandler] for a deep link among the handlers contributed by features, keyed by [DeepLinkKey].
 *
 * Candidate keys are built from the most specific to the least specific one, and the first registered key wins.
 * For `https://tangem.com/news/12-slug` the candidates are `https://tangem.com/news/12-slug`,
 * `https://tangem.com/news`, `https://tangem.com` and `https:`, so a handler of a nested path takes precedence
 * over a handler of its parent path.
 *
 * Scheme, host and path segments are compared case-sensitively, as received. A handler whose
 * [DeepLinkHandler.isEnabled] is `false` is skipped as if it were not registered.
 *
 * Registered keys are validated and normalized the same way as the candidates, so a trailing or doubled slash in a
 * [DeepLinkKey] does not matter. A malformed key, or two keys that normalize to the same one, fail the construction.
 */
class DeepLinkHandlerRegistry(handlers: Map<String, DeepLinkHandler>) {

    private val handlers: Map<String, DeepLinkHandler> = normalizeKeys(handlers)

    /** Deeper path prefixes can't match any key, so candidates are bounded by the deepest registered key. */
    private val maxPathSegments: Int = this.handlers.keys.maxOfOrNull(::pathSegmentsCount) ?: 0

    fun find(uri: Uri): DeepLinkHandler? {
        val candidateKeys = candidateKeys(
            scheme = uri.scheme,
            host = uri.host,
            pathSegments = uri.pathSegments,
            maxPathSegments = maxPathSegments,
        )
        val registeredKeys = candidateKeys.filter(handlers::containsKey)
        val key = registeredKeys.firstOrNull { handlers.getValue(it).isEnabled }

        if (key == null) {
            TangemLogger.i(
                """
                    No enabled deep link handler registered
                    |- Received URI: $uri
                    |- Candidate keys: $candidateKeys
                    |- Disabled keys: $registeredKeys
                """.trimIndent(),
            )
            return null
        }

        TangemLogger.i(
            """
                Found deep link handler
                |- Received URI: $uri
                |- Key: $key
                |- Skipped disabled keys: ${registeredKeys.takeWhile { it != key }}
            """.trimIndent(),
        )

        return handlers.getValue(key)
    }

    internal companion object {

        /** `scheme:` or `scheme://host[/path]`, without query and fragment. */
        private val KEY_REGEX = Regex("""^([A-Za-z][A-Za-z0-9+.\-]*):(?://([^/?#\s]+)([^?#\s]*))?$""")

        fun normalizeKey(key: String): String {
            val match = requireNotNull(KEY_REGEX.matchEntire(key)) {
                "Invalid deep link key '$key': expected 'scheme:', 'scheme://host' or 'scheme://host/path' " +
                    "without query and fragment"
            }
            val (scheme, host, path) = match.destructured

            return candidateKeys(scheme = scheme, host = host.ifEmpty { null }, pathSegments = path.split('/')).first()
        }

        private fun normalizeKeys(handlers: Map<String, DeepLinkHandler>): Map<String, DeepLinkHandler> {
            val keysByNormalized = handlers.keys.groupBy(::normalizeKey)
            val collisions = keysByNormalized.filterValues { it.size > 1 }
            require(collisions.isEmpty()) {
                "Deep link keys collide after normalization: ${collisions.values.joinToString()}"
            }

            return handlers.mapKeys { (key, _) -> normalizeKey(key) }
        }

        /** Number of path segments of a normalized key: `https://tangem.com/news/details` → 2, `wc:` → 0. */
        private fun pathSegmentsCount(normalizedKey: String): Int {
            return normalizedKey.substringAfter(delimiter = "://", missingDelimiterValue = "").count { it == '/' }
        }

        /**
         * [pathSegments] are decoded segments, as [Uri.getPathSegments] returns them. A segment with an encoded
         * slash (`%2F`) can't match a key segment, so no prefixes are built from it on.
         */
        fun candidateKeys(
            scheme: String?,
            host: String?,
            pathSegments: List<String>,
            maxPathSegments: Int = Int.MAX_VALUE,
        ): List<String> {
            if (scheme.isNullOrBlank()) return emptyList()

            val schemeKey = "$scheme:"
            if (host.isNullOrBlank()) return listOf(schemeKey)

            val hostKey = "$scheme://$host"
            val pathKeys = pathSegments
                .asSequence()
                .filter(String::isNotEmpty)
                .takeWhile { segment -> '/' !in segment }
                .take(maxPathSegments)
                .runningFold(hostKey) { prefix, segment -> "$prefix/$segment" }
                .drop(1)
                .toList()
                .asReversed()

            return pathKeys + hostKey + schemeKey
        }
    }
}