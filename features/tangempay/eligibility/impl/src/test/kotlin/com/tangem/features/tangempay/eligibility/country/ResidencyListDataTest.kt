package com.tangem.features.tangempay.eligibility.country

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.test.core.ProvideTestModels
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ResidencyListDataTest {

    @ParameterizedTest
    @ProvideTestModels
    fun toContent(model: SearchModel) {
        // Act
        val actual = LOADED.toContent(
            query = model.query,
            suggestedTitle = SUGGESTED_TITLE,
            allTitle = ALL_TITLE,
        )

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN null section titles WHEN content built THEN sections have no header`() {
        // Act
        val actual = LOADED.toContent(query = "", suggestedTitle = null, allTitle = null)

        // Assert
        assertThat(actual).isEqualTo(sectionsContent(suggestedTitle = null, allTitle = null))
    }

    @Test
    fun `GIVEN section titles WHEN content built THEN sections keep their headers`() {
        // Act
        val actual = LOADED.toContent(query = "", suggestedTitle = SUGGESTED_TITLE, allTitle = ALL_TITLE)

        // Assert
        val titles = (actual as ResidencyListUM.Content.Sections).sections.map(ResidencyListUM.SectionUM::title)
        assertThat(titles).containsExactly(SUGGESTED_TITLE, ALL_TITLE).inOrder()
    }

    private fun provideTestModels() = listOf(
        SearchModel(query = "us", expected = searchContent(UNITED_STATES)),
        SearchModel(query = " us ", expected = searchContent(UNITED_STATES)),
        SearchModel(query = "unit", expected = searchContent(UNITED_STATES, UNITED_KINGDOM)),
        SearchModel(query = "ny", expected = searchContent(NEW_YORK)),
        SearchModel(query = "zz", expected = ResidencyListUM.Content.Empty),
        SearchModel(query = "", expected = sectionsContent(SUGGESTED_TITLE, ALL_TITLE)),
    )

    internal data class SearchModel(val query: String, val expected: ResidencyListUM.Content)

    private fun searchContent(vararg items: ResidencyListUM.ItemUM): ResidencyListUM.Content =
        ResidencyListUM.Content.Sections(
            sections = persistentListOf(
                ResidencyListUM.SectionUM(
                    id = SEARCH_SECTION_ID,
                    title = null,
                    items = items.toList().toImmutableList(),
                ),
            ),
        )

    private fun sectionsContent(
        suggestedTitle: TextReference?,
        allTitle: TextReference?,
    ): ResidencyListUM.Content = ResidencyListUM.Content.Sections(
        sections = persistentListOf(
            ResidencyListUM.SectionUM(
                id = SUGGESTED_SECTION_ID,
                title = suggestedTitle,
                items = LOADED.suggested.toImmutableList(),
            ),
            ResidencyListUM.SectionUM(
                id = ALL_SECTION_ID,
                title = allTitle,
                items = LOADED.all.toImmutableList(),
            ),
        ),
    )

    private companion object {
        const val SUGGESTED_SECTION_ID = "suggested"
        const val ALL_SECTION_ID = "all"
        const val SEARCH_SECTION_ID = "search"

        val SUGGESTED_TITLE = stringReference("Suggested")
        val ALL_TITLE = stringReference("All")

        val UNITED_STATES = item(id = "US", name = "United States")
        val UNITED_KINGDOM = item(id = "GB", name = "United Kingdom")
        val MONACO = item(id = "MC", name = "Monaco")
        val NEW_YORK = item(id = "US-NY", name = "New York")
        val CALIFORNIA = item(id = "US-CA", name = "California")

        val LOADED = ResidencyListData.Loaded(
            suggested = listOf(UNITED_STATES),
            all = listOf(UNITED_STATES, UNITED_KINGDOM, MONACO, NEW_YORK, CALIFORNIA),
        )

        fun item(id: String, name: String): ResidencyListUM.ItemUM = ResidencyListUM.ItemUM(
            id = id,
            name = name,
            flagUrl = null,
            isLoading = false,
            onClick = {},
        )
    }
}