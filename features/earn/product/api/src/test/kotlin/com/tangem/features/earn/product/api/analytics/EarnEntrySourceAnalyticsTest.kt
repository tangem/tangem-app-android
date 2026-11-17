package com.tangem.features.earn.product.api.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.features.earn.api.EarnEntrySource
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnEntrySourceAnalyticsTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN entry source WHEN toAnalyticsValue THEN spec string`(model: ValueModel) {
        // Act
        val actual = model.input.toAnalyticsValue()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        ValueModel(EarnEntrySource.TokenPage, "Token Page"),
        ValueModel(EarnEntrySource.Markets, "Markets"),
        ValueModel(EarnEntrySource.Deeplink, "Deeplink"),
        ValueModel(EarnEntrySource.EarnBanner, "Earn Banner"),
        ValueModel(EarnEntrySource.YieldSheet, "Yield Sheet"),
        ValueModel(EarnEntrySource.ForYou, "For You"),
    )

    internal data class ValueModel(val input: EarnEntrySource, val expected: String) {
        override fun toString(): String = "$input -> $expected"
    }
}