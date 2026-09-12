package com.tangem.features.feed.rwa.categories

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class RwaCategoryNamesTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Category {

        @ParameterizedTest
        @ProvideTestModels
        fun category(model: CategoryModel) {
            // Act
            val actual = RwaCategoryNames.category(code = model.code, fallback = FALLBACK)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            CategoryModel(
                code = "bonds",
                expected = resourceReference(R.string.markets_real_assets_category_bonds),
            ),
            CategoryModel(
                code = "xstocks",
                expected = resourceReference(R.string.markets_real_assets_category_xstocks),
            ),
            CategoryModel(code = "not-shipped-yet", expected = stringReference(FALLBACK)),
                    )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Sector {

        @ParameterizedTest
        @ProvideTestModels
        fun sector(model: SectorModel) {
            // Act
            val actual = RwaCategoryNames.sector(
                categoryCode = model.categoryCode,
                sectorCode = model.sectorCode,
                fallback = FALLBACK,
            )

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            SectorModel(
                categoryCode = "xstocks",
                sectorCode = "technology",
                expected = resourceReference(R.string.markets_real_assets_sector_xstocks_technology),
            ),
            SectorModel(
                categoryCode = "xstocks",
                sectorCode = "healthcare",
                expected = resourceReference(R.string.markets_real_assets_sector_xstocks_healthcare),
            ),
            // the same sector name under another category has its own key, never the xStocks one
            SectorModel(categoryCode = "bonds", sectorCode = "technology", expected = stringReference(FALLBACK)),
            SectorModel(categoryCode = "xstocks", sectorCode = "not-shipped-yet", expected = stringReference(FALLBACK)),
        )
    }

    internal data class CategoryModel(val code: String, val expected: TextReference)

    internal data class SectorModel(val categoryCode: String, val sectorCode: String, val expected: TextReference)

    private companion object {
        const val FALLBACK = "Backend name"
    }
}