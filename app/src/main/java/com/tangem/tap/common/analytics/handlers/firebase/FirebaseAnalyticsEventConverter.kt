package com.tangem.tap.common.analytics.handlers.firebase

internal class FirebaseAnalyticsEventConverter {

    fun convertEventName(event: String): String {
        return convertString(event, FIREBASE_EVENT_NAME_MAX_LENGTH)
    }

    fun convertEventParams(params: Map<String, String>): Map<String, String> {
        return params.map { (key, value) ->
            convertString(key, FIREBASE_EVENT_NAME_MAX_LENGTH) to convertString(value, FIREBASE_EVENT_VALUE_MAX_LENGTH)
        }.toMap()
    }

    private fun convertString(string: String, maxLength: Int): String {
        return string
            .replace(REPLACING_PATTERN.toRegex(), WORD_SEPARATOR)
            .trim { it in TRIMMING_CHARACTERS }
            .trimToLength(maxLength)
    }

    private fun String.trimToLength(length: Int): String {
        return if (this.length > length) this.substring(0, length) else this
    }

    private companion object {
        const val REPLACING_PATTERN = "[^\\w]+"
        const val WORD_SEPARATOR = "_"
        const val TRIMMING_CHARACTERS = WORD_SEPARATOR
        const val FIREBASE_EVENT_NAME_MAX_LENGTH = 40
        const val FIREBASE_EVENT_VALUE_MAX_LENGTH = 100
    }
}