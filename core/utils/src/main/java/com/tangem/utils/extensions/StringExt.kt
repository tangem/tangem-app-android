package com.tangem.utils.extensions

private const val DEEPLINK_VALIDATION_REGEX = "[\";<>()+\\\\]"

/**
 * Check for malicious symbol in uri part
 */
fun String.uriValidate(): Boolean {
    val regex = DEEPLINK_VALIDATION_REGEX.toRegex()

    return !regex.containsMatchIn(this)
}