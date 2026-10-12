package com.tangem.features.feed.crypto.categories.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.markets.MarketCategory
import com.tangem.features.feed.crypto.categories.MarketCategorySelection
import com.tangem.features.feed.crypto.categories.createCategory
import com.tangem.features.feed.crypto.categories.selectionOf
import com.tangem.features.feed.crypto.categories.ui.state.FeaturedCategoriesUM
import com.tangem.features.feed.crypto.categories.ui.state.FeaturedCategoryUM
import com.tangem.features.feed.crypto.impl.R
import org.junit.jupiter.api.Test

/**
 * The rail is a shortcut into the same list, so a badge selects exactly what its row does.
 *
 * It parts ways with the row on one point: a tap on the selected badge clears the filter, where the
 * selected row republishes it. The rail has no "All" badge to clear through, so the badge has to
 * toggle. See [MarketCategoryItemConverterTest] for the other half.
 */
internal class FeaturedCategoryConverterTest {

    private var emitted: MarketCategorySelection? = null

    private val ai = createCategory(id = "ai", name = "AI", isFeatured = true)
    private val meme = createCategory(id = "meme", name = "Meme", isFeatured = true)

    @Test
    fun `GIVEN featured categories WHEN converted THEN the rail keeps their order under the featured title`() {
        // Act
        val actual = convert(ai, meme)

        // Assert
        assertThat(actual.title).isEqualTo(TextReference.Res(R.string.common_featured))
        assertThat(actual.items.map(FeaturedCategoryUM::id)).containsExactly("ai", "meme").inOrder()
        assertThat(actual.items.map(FeaturedCategoryUM::label))
            .containsExactly(TextReference.Str("AI"), TextReference.Str("Meme")).inOrder()
    }

    @Test
    fun `GIVEN no featured categories WHEN converted THEN the rail is empty`() {
        // Act
        val actual = convert()

        // Assert
        assertThat(actual.items).isEmpty()
    }

    @Test
    fun `GIVEN nothing selected WHEN converted THEN no badge is checked`() {
        // Act
        val actual = convert(ai, meme)

        // Assert
        assertThat(actual.items.none(FeaturedCategoryUM::isSelected)).isTrue()
    }

    @Test
    fun `GIVEN a category selected WHEN converted THEN only its badge is checked`() {
        // Act
        val actual = convert(ai, meme, selection = selectionOf(ai))

        // Assert
        assertThat(actual.items.filter(FeaturedCategoryUM::isSelected).map(FeaturedCategoryUM::id))
            .containsExactly("ai")
    }

    @Test
    fun `GIVEN a selection carrying a stale name WHEN converted THEN the badge is still matched by id`() {
        // Arrange — a category renamed on the backend must not lose its checkmark
        val renamed = MarketCategorySelection(MarketCategorySelection.Entry(id = "ai", name = "Artificial Intelligence"))

        // Act
        val actual = convert(ai, meme, selection = renamed)

        // Assert
        assertThat(actual.items.filter(FeaturedCategoryUM::isSelected).map(FeaturedCategoryUM::id))
            .containsExactly("ai")
    }

    @Test
    fun `GIVEN an unselected badge WHEN tapped THEN its id and name are published`() {
        // Arrange
        val rail = convert(ai, meme)

        // Act
        rail.badgeFor("meme").onItemClick()

        // Assert
        assertThat(emitted).isEqualTo(MarketCategorySelection(MarketCategorySelection.Entry("meme", "Meme")))
    }

    @Test
    fun `GIVEN the selected badge WHEN tapped again THEN the filter is cleared`() {
        // Arrange — the rail has no All badge, so the badge itself has to toggle off
        val rail = convert(ai, meme, selection = selectionOf(ai))

        // Act
        rail.badgeFor("ai").onItemClick()

        // Assert
        assertThat(emitted).isEqualTo(MarketCategorySelection())
    }

    private fun convert(
        vararg categories: MarketCategory,
        selection: MarketCategorySelection = MarketCategorySelection(),
    ): FeaturedCategoriesUM = FeaturedCategoryConverter(
        selection = selection,
        onSelectionChange = { emitted = it },
    ).convert(categories.toList())

    private fun FeaturedCategoriesUM.badgeFor(id: String): FeaturedCategoryUM = items.single { it.id == id }
}