package com.tangem.features.tangempay.eligibility.country

import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

internal sealed interface ResidencyListData {

    data object Loading : ResidencyListData

    data class Loaded(
        val suggested: List<ResidencyListUM.ItemUM>,
        val all: List<ResidencyListUM.ItemUM>,
    ) : ResidencyListData

    data class Failed(val onRetry: () -> Unit) : ResidencyListData
}

internal fun ResidencyListData.toContent(
    query: String,
    suggestedTitle: TextReference?,
    allTitle: TextReference?,
): ResidencyListUM.Content {
    val loaded = when (this) {
        ResidencyListData.Loading -> return ResidencyListUM.Content.Loading
        is ResidencyListData.Failed -> return ResidencyListUM.Content.Error(onRetryClick = onRetry)
        is ResidencyListData.Loaded -> this
    }

    val trimmedQuery = query.trim()
    if (trimmedQuery.isNotEmpty()) {
        val found = loaded.all.filter { item -> item.matches(trimmedQuery) }
        return if (found.isEmpty()) {
            ResidencyListUM.Content.Empty
        } else {
            ResidencyListUM.Content.Sections(
                sections = persistentListOf(
                    ResidencyListUM.SectionUM(
                        id = SEARCH_SECTION_ID,
                        title = null,
                        items = found.toImmutableList(),
                    ),
                ),
            )
        }
    }

    val sections = buildList {
        if (loaded.suggested.isNotEmpty()) {
            add(
                ResidencyListUM.SectionUM(
                    id = SUGGESTED_SECTION_ID,
                    title = suggestedTitle,
                    items = loaded.suggested.toImmutableList(),
                ),
            )
        }
        add(
            ResidencyListUM.SectionUM(
                id = ALL_SECTION_ID,
                title = allTitle,
                items = loaded.all.toImmutableList(),
            ),
        )
    }

    return ResidencyListUM.Content.Sections(sections = sections.toImmutableList())
}

private const val SUGGESTED_SECTION_ID = "suggested"
private const val ALL_SECTION_ID = "all"
private const val SEARCH_SECTION_ID = "search"

/** A code prefix finds the row the way a name substring does: "us" → United States, "ny" → New York. */
private fun ResidencyListUM.ItemUM.matches(query: String): Boolean =
    name.contains(query, ignoreCase = true) || subdivisionCode.startsWith(query, ignoreCase = true)

/** Regions arrive as ISO 3166-2 ("US-NY"), so the country part never shadows the subdivision. */
private val ResidencyListUM.ItemUM.subdivisionCode: String get() = id.substringAfterLast(delimiter = '-')