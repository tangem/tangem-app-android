package com.tangem.core.featuretoggle.contract

import timber.log.Timber

/**
 * Presentation of application version (<major>.<minor>.<fix?>).
 *
 * @param value version value as string
 *
* [REDACTED_AUTHOR]
 */
internal class Version private constructor(value: String) : Comparable<Version> {

    private val major: Int
    private val minor: Int
    private val fix: Int?

    init {
        val versions = value.split(VERSION_DELIMITER).map(String::toInt)

        major = versions.getVersionValue(index = MAJOR_VERSION_POSITION)
        minor = versions.getVersionValue(index = MINOR_VERSION_POSITION)
        fix = versions.getOrNull(index = FIX_VERSION_POSITION)
    }

    override fun compareTo(other: Version): Int {
        var result = major.compareTo(other.major)
        if (result == 0) result = minor.compareTo(other.minor)
        if (result == 0) {
            when {
                fix == null && other.fix == null -> result = 0
                fix == null && other.fix != null -> result = -1
                fix != null && other.fix == null -> result = 1
                fix != null && other.fix != null -> result = fix.compareTo(other.fix)
            }
        }
        return result
    }

    private fun List<Int>.getVersionValue(index: Int): Int {
        return getOrNull(index) ?: error("Invalid version")
    }

    companion object {
        private const val MAJOR_VERSION_POSITION = 0
        private const val MINOR_VERSION_POSITION = 1
        private const val FIX_VERSION_POSITION = 2
        private const val VERSION_DELIMITER = "."

        /**
         * Create instance with value [value].
         * If [value] doesn't meet all requirements, the function returns null.
         */
        fun create(value: String): Version? {
            return try {
                Version(value)
            } catch (exception: Exception) {
                Timber.e(exception, "Version %s is null", value)
                return null
            }
        }
    }
}
