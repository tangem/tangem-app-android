package com.tangem.features.feed.crypto.categories.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.BigDecimalFormatConstants
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.MarketCategory
import com.tangem.features.feed.crypto.categories.MarketCategorySelection
import com.tangem.features.feed.crypto.categories.createCategory
import com.tangem.features.feed.crypto.categories.selectionOf
import com.tangem.features.feed.crypto.categories.ui.state.MarketCategoryItemUM
import org.junit.jupiter.api.Test

/**
 * The rows are a radio group over the categories alone — there is no "no filter" row, so an unfiltered
 * list checks nothing and clearing lives on the tab chip.
 *
 * A tap on the already-selected row deliberately republishes the same selection instead of clearing it;
 * the featured badge does the opposite. See [FeaturedCategoryConverterTest] for the other half.
 */
internal class MarketCategoryItemConverterTest {

    private var emitted: MarketCategorySelection? = null

    private val ai = createCategory(id = "ai", name = "AI")
    private val defi = createCategory(id = "defi", name = "DeFi", tokensCount = 7)

    @Test
    fun `GIVEN nothing selected WHEN converted THEN only the categories are built`() {
        // Act
        val actual = convert(ai, defi)

        // Assert
        assertThat(actual.map(MarketCategoryItemUM::id)).containsExactly("ai", "defi").inOrder()
    }

    @Test
    fun `GIVEN no categories WHEN converted THEN no rows are built`() {
        // Act
        val actual = convert()

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN nothing selected WHEN converted THEN no row is checked`() {
        // Act
        val actual = convert(ai, defi)

        // Assert
        assertThat(actual.none(MarketCategoryItemUM::isSelected)).isTrue()
    }

    @Test
    fun `GIVEN a category selected WHEN converted THEN only its row is checked`() {
        // Act
        val actual = convert(ai, defi, selection = selectionOf(ai))

        // Assert
        assertThat(actual.filter(MarketCategoryItemUM::isSelected).map(MarketCategoryItemUM::id))
            .containsExactly("ai")
    }

    @Test
    fun `GIVEN a selection missing from the page WHEN converted THEN no row is checked`() {
        // Arrange — a stale id survives a page that no longer carries it
        val stale = selectionOf(createCategory(id = "gone", name = "Gone"))

        // Act
        val actual = convert(ai, defi, selection = stale)

        // Assert
        assertThat(actual.none(MarketCategoryItemUM::isSelected)).isTrue()
    }

    @Test
    fun `GIVEN an unselected row WHEN tapped THEN its id and name are published`() {
        // Arrange
        val rows = convert(ai, defi)

        // Act
        rows.rowFor("defi").onItemClick()

        // Assert
        assertThat(emitted).isEqualTo(MarketCategorySelection(MarketCategorySelection.Entry("defi", "DeFi")))
    }

    @Test
    fun `GIVEN the selected row WHEN tapped again THEN the selection is republished unchanged`() {
        // Arrange — a row that also deselects reads as a broken radio group; clearing is the chip's job
        val selection = selectionOf(ai)
        val rows = convert(ai, defi, selection = selection)

        // Act
        rows.rowFor("ai").onItemClick()

        // Assert
        assertThat(emitted).isEqualTo(selection)
    }

    @Test
    fun `GIVEN a category WHEN converted THEN its title and coin count fill the row`() {
        // Act
        val actual = convert(ai, defi).rowFor("defi")

        // Assert
        assertThat(actual.title).isEqualTo(TextReference.Str("DeFi"))
        assertThat(actual.caption).isEqualTo(
            TextReference.PluralRes(
                id = R.plurals.common_assets_count,
                count = 7,
                formatArgs = wrappedList(7),
            ),
        )
    }

    @Test
    fun `GIVEN a category without a market cap WHEN converted THEN the subtitle falls back to a dash`() {
        // Act
        val actual = convert(ai).rowFor("ai")

        // Assert
        assertThat(actual.subtitle)
            .isEqualTo(TextReference.Str(BigDecimalFormatConstants.EMPTY_BALANCE_SIGN))
    }

    private fun convert(
        vararg categories: MarketCategory,
        selection: MarketCategorySelection = MarketCategorySelection(),
        appCurrency: AppCurrency = AppCurrency.Default,
    ): List<MarketCategoryItemUM> = MarketCategoryItemConverter(
        selection = selection,
        appCurrency = appCurrency,
        onSelectionChange = { emitted = it },
    ).convert(categories.toList())

    private fun List<MarketCategoryItemUM>.rowFor(id: String): MarketCategoryItemUM = single { it.id == id }
}