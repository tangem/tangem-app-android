package com.tangem.features.commonfeatures.impl.tokenactions.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.features.commonfeatures.impl.R
import org.junit.jupiter.api.Test
import java.util.Locale

internal class TokenActionsRegionRestrictionTest {

    @Test
    fun `GIVEN a resolvable country WHEN banner THEN the notice names it`() {
        // Arrange
        val originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.ENGLISH)

        try {
            // Act
            val actual = TokenActionsRegionRestriction.banner(countryCode = "gb")

            // Assert
            assertThat(actual.description).isEqualTo(
                resourceReference(
                    id = R.string.buy_swap_region_restriction_description_region,
                    formatArgs = wrappedList("United Kingdom"),
                ),
            )
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun `GIVEN no resolved country WHEN banner THEN the notice drops the region`() {
        // Act
        val actual = TokenActionsRegionRestriction.banner(countryCode = null)

        // Assert
        assertThat(actual.description)
            .isEqualTo(resourceReference(R.string.buy_swap_region_restriction_description))
    }

    @Test
    fun `GIVEN an unassigned country code WHEN banner THEN the notice drops the region`() {
        // Act
        val actual = TokenActionsRegionRestriction.banner(countryCode = "QQ")

        // Assert
        assertThat(actual.description)
            .isEqualTo(resourceReference(R.string.buy_swap_region_restriction_description))
    }

    @Test
    fun `GIVEN any country WHEN banner THEN it stays a warning titled with the restriction`() {
        // Act
        val actual = TokenActionsRegionRestriction.banner(countryCode = "US")

        // Assert
        assertThat(actual.variant).isEqualTo(TangemMessageBanner.Variant.Warning)
        assertThat(actual.title).isEqualTo(resourceReference(R.string.buy_swap_region_restriction_title))
    }
}