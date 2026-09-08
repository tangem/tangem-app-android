package com.tangem.features.feed.crypto.categories.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.BigDecimalFormatConstants
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.MarketCategory
import com.tangem.features.feed.crypto.categories.ALL_ROW_ID
import com.tangem.features.feed.crypto.categories.MarketCategorySelection
import com.tangem.features.feed.crypto.categories.createCategory
import com.tangem.features.feed.crypto.categories.selectionOf
import com.tangem.features.feed.crypto.categories.ui.state.MarketCategoryItemUM
import org.junit.jupiter.api.Test

/**
 * The rows are a radio group over "no filter" plus every category, so the invariants worth pinning are
 * that exactly one row is checked and that clearing has a single route — the "All" row.
 *
 * A tap on the already-selected row deliberately republishes the same selection instead of clearing it;
 * the featured badge does the opposite. See [FeaturedCategoryConverterTest] for the other half.
 */
internal class MarketCategoryItemConverterTest {

    private var emitted: MarketCategorySelection? = null

    private val ai = createCategory(id = "ai", name = "AI")
    private val defi = createCategory(id = "defi", name = "DeFi", tokensCount = 7)

    @Test
    fun `GIVEN categories WHEN converted THEN the All row leads and the categories keep their order`() {
        // Act
        val actual = convert(ai, defi)

        // Assert
        assertThat(actual.map(MarketCategoryItemUM::id)).containsExactly(ALL_ROW_ID, "ai", "defi").inOrder()
    }

    @Test
    fun `GIVEN no categories WHEN converted THEN only the All row is built`() {
        // Act
        val actual = convert()

        // Assert
        assertThat(actual.map(MarketCategoryItemUM::id)).containsExactly(ALL_ROW_ID)
    }

    @Test
    fun `GIVEN nothing selected WHEN converted THEN the All row is the checked one`() {
        // Act
        val actual = convert(ai, defi)

        // Assert
        assertThat(actual.filter(MarketCategoryItemUM::isSelected).map(MarketCategoryItemUM::id))
            .containsExactly(ALL_ROW_ID)
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
    fun `GIVEN a filter applied WHEN the All row is tapped THEN the empty selection is published`() {
        // Arrange
        val rows = convert(ai, defi, selection = selectionOf(ai))

        // Act
        rows.rowFor(ALL_ROW_ID).onItemClick()

        // Assert
        assertThat(emitted).isEqualTo(MarketCategorySelection())
    }

    @Test
    fun `GIVEN the selected row WHEN tapped again THEN the selection is republished unchanged`() {
        // Arrange — clearing belongs to the All row; a row that also deselects reads as a broken radio group
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

    @Test
    fun `GIVEN the All row WHEN converted THEN it carries no market data`() {
        // Act
        val actual = convert(ai).rowFor(ALL_ROW_ID)

        // Assert
        assertThat(actual.title).isEqualTo(TextReference.Res(R.string.common_all))
        assertThat(actual.subtitle).isNull()
        assertThat(actual.caption).isNull()
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