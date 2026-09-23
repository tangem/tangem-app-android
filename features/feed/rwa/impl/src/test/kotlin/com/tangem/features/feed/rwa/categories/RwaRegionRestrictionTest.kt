package com.tangem.features.feed.rwa.categories

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.domain.markets.CoinCategory
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import com.tangem.test.core.ProvideTestModels

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RwaRegionRestrictionTest {

    @Test
    fun `GIVEN a restricted visible category WHEN restrictedIds THEN its id is collected`() {
        // Arrange
        val categories = listOf(
            category(id = "1", code = "xstocks", isRestricted = true),
            category(id = "2", code = "commodities", isRestricted = false),
        )

        // Act
        val actual = RwaRegionRestriction.restrictedIds(categories)

        // Assert
        assertThat(actual).containsExactly("1")
    }

    @Test
    fun `GIVEN a restricted hidden category WHEN restrictedIds THEN it is skipped`() {
        // Arrange — a token's own flag aggregates visible categories only, so a hidden one must not warn
        val categories = listOf(category(id = "1", code = "featured-assets", isRestricted = true, isVisible = false))

        // Act
        val actual = RwaRegionRestriction.restrictedIds(categories)

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN a restricted pick WHEN banner THEN the notice is built`() {
        // Act
        val actual = RwaRegionRestriction.banner(
            restrictedIds = setOf("1"),
            selection = CategorySelection(CategorySelection.Entry(id = "1", displayName = "xStocks", code = "xstocks")),
        )

        // Assert
        assertThat(actual?.variant).isEqualTo(TangemMessageBanner.Variant.Warning)
    }

    @Test
    fun `GIVEN an allowed pick WHEN banner THEN there is no notice`() {
        // Act
        val actual = RwaRegionRestriction.banner(
            restrictedIds = setOf("1"),
            selection = CategorySelection(CategorySelection.Entry(id = "2", displayName = "Bonds", code = "bonds")),
        )

        // Assert
        assertThat(actual).isNull()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun isRestricted(model: IsRestrictedModel) {
        // Act
        val actual = RwaRegionRestriction.isRestricted(
            restrictedIds = model.restrictedIds,
            selection = model.selection,
        )

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class IsRestrictedModel(
        val restrictedIds: Set<String>,
        val selection: CategorySelection,
        val expected: Boolean,
    )

    private fun provideTestModels() = listOf(
        IsRestrictedModel(
            restrictedIds = setOf("1"),
            selection = CategorySelection(category = CategorySelection.Entry(id = "1", displayName = "xStocks", code = "xstocks")),
            expected = true,
        ),
        IsRestrictedModel(
            restrictedIds = setOf("1"),
            selection = CategorySelection(category = CategorySelection.Entry(id = "2", displayName = "Commodities", code = "commodities")),
            expected = false,
        ),
        // the unfiltered scope names no category, so the notice stays silent there
        IsRestrictedModel(
            restrictedIds = setOf("1"),
            selection = CategorySelection(),
            expected = false,
        ),
        IsRestrictedModel(
            restrictedIds = emptySet(),
            selection = CategorySelection(),
            expected = false,
        ),
        // a sector narrows its category, it never lifts the category's restriction
        IsRestrictedModel(
            restrictedIds = setOf("1"),
            selection = CategorySelection(
                category = CategorySelection.Entry(id = "1", displayName = "xStocks", code = "xstocks"),
                sector = CategorySelection.Entry(id = "tech", displayName = "Technology", code = "technology"),
            ),
            expected = true,
        ),
    )

    private fun category(id: String, code: String, isRestricted: Boolean, isVisible: Boolean = true) = CoinCategory(
        id = id,
        code = code,
        displayName = code,
        displayOrder = 0,
        tokensCount = 7,
        isRestricted = isRestricted,
        sectors = emptyList(),
        isVisible = isVisible,
    )
}