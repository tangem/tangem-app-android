package com.tangem.features.feed.rwa.categories

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.domain.markets.CoinCategory
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import com.tangem.test.core.ProvideTestModels
import java.util.Locale

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
            country = "US",
        )

        // Assert
        assertThat(actual?.variant).isEqualTo(TangemMessageBanner.Variant.Warning)
    }

    @Test
    fun `GIVEN a resolvable country WHEN banner THEN the notice names it`() {
        // Arrange — the code is what the app sends, the name is what a user can read
        val originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.ENGLISH)

        try {
            // Act
            val actual = banner(country = "GB")

            // Assert
            assertThat(actual?.description).isEqualTo(
                resourceReference(
                    R.string.markets_real_assets_region_restriction_description_region,
                    wrappedList("United Kingdom"),
                ),
            )
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun `GIVEN no resolved country WHEN banner THEN the notice drops the region`() {
        // Act — the restriction is real either way, only its wording degrades
        val actual = banner(country = null)

        // Assert
        assertThat(actual?.description)
            .isEqualTo(resourceReference(R.string.markets_real_assets_region_restriction_description))
    }

    @Test
    fun `GIVEN an unassigned country code WHEN banner THEN the notice drops the region`() {
        // Act — a bare code reads worse than saying nothing
        val actual = banner(country = "QQ")

        // Assert
        assertThat(actual?.description)
            .isEqualTo(resourceReference(R.string.markets_real_assets_region_restriction_description))
    }

    @Test
    fun `GIVEN a region the platform names but is not a country WHEN banner THEN the region is dropped`() {
        // Act — CLDR calls XA "Pseudo-Accents", which must not reach the copy as a region
        val actual = banner(country = "XA")

        // Assert
        assertThat(actual?.description)
            .isEqualTo(resourceReference(R.string.markets_real_assets_region_restriction_description))
    }

    @Test
    fun `GIVEN an allowed pick WHEN banner THEN there is no notice`() {
        // Act
        val actual = RwaRegionRestriction.banner(
            restrictedIds = setOf("1"),
            selection = CategorySelection(CategorySelection.Entry(id = "2", displayName = "Bonds", code = "bonds")),
            country = "US",
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

    private fun banner(country: String?) = RwaRegionRestriction.banner(
        restrictedIds = setOf("1"),
        selection = CategorySelection(CategorySelection.Entry(id = "1", displayName = "xStocks", code = "xstocks")),
        country = country,
    )
}