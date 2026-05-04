package com.tangem.utils.logging

import java.util.regex.Pattern

/**
 * Resolves a log tag from the call site's class name when the caller didn't supply one
 * via [TangemLogger.withTag]. Used by [TangemLogger.write] before dispatching to writers.
 */
internal object LogTagResolver {

    private const val FALLBACK_TAG = "TangemAppLogger"

    private val ANONYMOUS_CLASS_REGEX: Pattern = Pattern.compile("(\\$\\d+)+$")

    private val FQCN_IGNORE = setOf(
        LogTagResolver::class.java.name,
        TangemLogger::class.java.name,
        TangemLogger.TaggedLogger::class.java.name,
        // Synthetic class generated for BaseLogger's default-arg trampolines (d$default, etc.).
        // Without this, every call that omits default args resolves to BaseLogger.DefaultImpls.
        "${BaseLogger::class.java.name}\$DefaultImpls",
    )

    @Suppress("ThrowingExceptionsWithoutMessageOrCause")
    fun resolveTag(): String {
        val element = Throwable().stackTrace.firstOrNull { it.className !in FQCN_IGNORE }
            ?: return FALLBACK_TAG
        var tag = element.className.substringAfterLast('.')
        val matcher = ANONYMOUS_CLASS_REGEX.matcher(tag)
        if (matcher.find()) {
            tag = matcher.replaceAll("")
        }
        return tag
    }
}