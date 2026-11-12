package com.tangem.features.feed.v2

/**
 * Filter picker of a feed tab that can be opened from outside the app — the `filter` parameter of the
 * `tangem://discover` deeplink. Each value exists only on specific sections, see [resolve].
 */
enum class FeedFilterSheet(val value: String) {

    Category(value = "category"),
    Network(value = "network"),
    Type(value = "type"),
    ;

    companion object {

        /** `null` for an unknown filter, or one the [section] has no picker for — the section then opens bare. */
        fun resolve(section: FeedSection?, value: String?): FeedFilterSheet? {
            val sheet = entries.firstOrNull { it.value == value } ?: return null

            val isOffered = when (section) {
                FeedSection.Crypto, FeedSection.Assets -> sheet == Category
                FeedSection.Earn -> sheet == Network || sheet == Type
                null -> false
            }

            return sheet.takeIf { isOffered }
        }
    }
}