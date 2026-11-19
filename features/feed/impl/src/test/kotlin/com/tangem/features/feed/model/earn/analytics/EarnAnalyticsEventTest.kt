package com.tangem.features.feed.model.earn.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnAnalyticsEventTest {

    @ParameterizedTest
    @ProvideTestModels
    fun filterTypeApplied(model: FilterTypeModel) {
        // Act
        val event = EarnAnalyticsEvent.BestOpportunitiesFilterTypeApplied(filterTypeAnalytic = model.analytic)

        // Assert
        assertThat(event.id).isEqualTo("[Earn] Best Opportunities Filter Type Applied")
        assertThat(event.params).isEqualTo(mapOf("Type" to model.expectedType))
    }

    private fun provideTestModels() = listOf(
        FilterTypeModel(analytic = FilterTypeAnalytic.ALL_TYPES, expectedType = "All Types"),
        FilterTypeModel(analytic = FilterTypeAnalytic.STAKING, expectedType = "Staking"),
        FilterTypeModel(analytic = FilterTypeAnalytic.YIELD, expectedType = "Yield"),
        FilterTypeModel(analytic = FilterTypeAnalytic.VAULTS, expectedType = "Vaults"),
    )

    internal data class FilterTypeModel(val analytic: FilterTypeAnalytic, val expectedType: String) {
        override fun toString(): String =
            "GIVEN the $analytic filter WHEN the type filter event is built THEN Type is $expectedType"
    }
}