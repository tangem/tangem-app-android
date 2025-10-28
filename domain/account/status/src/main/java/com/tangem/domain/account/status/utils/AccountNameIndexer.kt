package com.tangem.domain.account.status.utils

import com.tangem.domain.models.account.AccountName

/**
 * Utility object for indexing account names to ensure uniqueness.
 *
 * @see [iOS](https://github.com/tangem-developments/tangem-app-ios/blob/32c56f1a566ecec00364ffeaf6b3c9929419ad15/Tangem/Domain/Accounts/Archiving/Utils/UnarchivedCryptoAccountNameIndexer.swift#L11)
 *
[REDACTED_AUTHOR]
 */
internal object AccountNameIndexer {

    private const val INITIAL_INDEX = 1
    private const val INDEX_PREFIX = "("
    private const val INDEX_SUFFIX = ")"

    /**
     * Transforms the given account name by appending or incrementing an index suffix to ensure uniqueness.
     *
     * @param from the original account name
     *
     * @return the transformed account name with an updated index suffix
     */
    fun transform(from: String): String {
        val (newIndex, currentIndex) = extractIndices(from)
        val accountNameSuffix = makeStringFromIndex(newIndex)
        val accountNamePrefixLength = (AccountName.MAX_LENGTH - accountNameSuffix.length).coerceAtLeast(0)

        var accountNamePrefix = from
        if (currentIndex != null) {
            val accountNameCurrentSuffix = makeStringFromIndex(currentIndex)
            val accountNameCurrentSuffixLength = accountNameCurrentSuffix.length
            accountNamePrefix = accountNamePrefix.dropLast(accountNameCurrentSuffixLength)
        }

        accountNamePrefix = accountNamePrefix.take(accountNamePrefixLength)

        return accountNamePrefix + accountNameSuffix
    }

    private fun extractIndices(string: String): Pair<Int, Int?> {
        val pattern = """\${INDEX_PREFIX}(\d+)\${INDEX_SUFFIX}$"""
        val regex = Regex(pattern)
        val match = regex.find(string)

        return if (match != null && match.groupValues.size > 1) {
            val currentIndex = match.groupValues[1].toIntOrNull()

            if (currentIndex != null) {
                Pair(currentIndex + 1, currentIndex)
            } else {
                Pair(INITIAL_INDEX, null)
            }
        } else {
            Pair(INITIAL_INDEX, null)
        }
    }

    private fun makeStringFromIndex(index: Int): String {
        return "${INDEX_PREFIX}$index${INDEX_SUFFIX}"
    }
}