package com.tangem.domain.appupdate.model

internal class AppVersion private constructor(
    private val major: Int,
    private val minor: Int,
    private val fix: Int,
) : Comparable<AppVersion> {

    override fun compareTo(other: AppVersion): Int {
        major.compareTo(other.major).let { if (it != 0) return it }
        minor.compareTo(other.minor).let { if (it != 0) return it }
        return fix.compareTo(other.fix)
    }

    companion object {
        private const val DELIMITER = "."
        private const val MAJOR = 0
        private const val MINOR = 1
        private const val FIX = 2

        fun parseOrNull(value: String): AppVersion? {
            // Drop build-type/pre-release suffixes ("6.1-internal", "1.0.0-SNAPSHOT") before parsing.
            val parts = value.trim().substringBefore('-').substringBefore('+').split(DELIMITER)

            val major = parts.getOrNull(MAJOR)?.toIntOrNull() ?: return null
            val minor = parts.getOrNull(MINOR).toVersionPartOrNull() ?: return null
            val fix = parts.getOrNull(FIX).toVersionPartOrNull() ?: return null

            return AppVersion(major = major, minor = minor, fix = fix)
        }

        private fun String?.toVersionPartOrNull(): Int? = when (this) {
            null -> 0
            else -> toIntOrNull()
        }
    }
}