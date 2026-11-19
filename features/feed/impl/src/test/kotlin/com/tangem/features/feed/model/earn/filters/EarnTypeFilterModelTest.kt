package com.tangem.features.feed.model.earn.filters

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.earn.model.EarnFilterType
import com.tangem.features.earn.api.EarnFeatureToggles
import com.tangem.features.feed.components.earn.EarnTypeFilterComponent
import com.tangem.features.feed.model.earn.analytics.EarnAnalyticsEvent
import com.tangem.features.feed.model.earn.analytics.FilterTypeAnalytic
import com.tangem.features.feed.ui.earn.state.EarnFilterTypeUM
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnTypeFilterModelTest {

    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val earnFeatureToggles: EarnFeatureToggles = mockk()

    private var selectedFilter: EarnFilterType? = null

    @BeforeEach
    fun reset() {
        clearMocks(analyticsEventHandler, earnFeatureToggles)
        every { earnFeatureToggles.isEarnEnabled } returns true
        selectedFilter = null
    }

    private fun createModel(selected: EarnFilterType = EarnFilterType.ALL) = EarnTypeFilterModel(
        dispatchers = TestingCoroutineDispatcherProvider(),
        analyticsEventHandler = analyticsEventHandler,
        earnFeatureToggles = earnFeatureToggles,
        paramsContainer = MutableParamsContainer(
            EarnTypeFilterComponent.Params(
                selectedFilter = selected,
                onFilterSelected = { selectedFilter = it },
                onDismiss = {},
            ),
        ),
    )

    @Test
    fun `GIVEN the Earn flow is on WHEN the sheet opens THEN it offers every type including vaults`() {
        // Act
        val actual = createModel()

        // Assert
        assertThat(actual.state.value.options).containsExactly(
            EarnFilterTypeUM.All,
            EarnFilterTypeUM.Staking,
            EarnFilterTypeUM.YieldMode,
            EarnFilterTypeUM.Vaults,
        ).inOrder()
    }

    @Test
    fun `GIVEN the Earn flow is off WHEN the sheet opens THEN vaults are not offered`() {
        // Arrange
        every { earnFeatureToggles.isEarnEnabled } returns false

        // Act
        val actual = createModel()

        // Assert
        assertThat(actual.state.value.options).containsExactly(
            EarnFilterTypeUM.All,
            EarnFilterTypeUM.Staking,
            EarnFilterTypeUM.YieldMode,
        ).inOrder()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun onOptionClick(model: OptionModel) {
        // Arrange
        val underTest = createModel()

        // Act
        underTest.state.value.onOptionClick(model.option)

        // Assert
        assertThat(selectedFilter).isEqualTo(model.filter)
        verify(exactly = 1) {
            analyticsEventHandler.send(EarnAnalyticsEvent.BestOpportunitiesFilterTypeApplied(model.analytic))
        }
    }

    private fun provideTestModels() = listOf(
        OptionModel(
            filter = EarnFilterType.ALL,
            option = EarnFilterTypeUM.All,
            analytic = FilterTypeAnalytic.ALL_TYPES,
        ),
        OptionModel(
            filter = EarnFilterType.STAKING,
            option = EarnFilterTypeUM.Staking,
            analytic = FilterTypeAnalytic.STAKING,
        ),
        OptionModel(
            filter = EarnFilterType.YIELD,
            option = EarnFilterTypeUM.YieldMode,
            analytic = FilterTypeAnalytic.YIELD,
        ),
        OptionModel(
            filter = EarnFilterType.VAULT,
            option = EarnFilterTypeUM.Vaults,
            analytic = FilterTypeAnalytic.VAULTS,
        ),
    )

    internal data class OptionModel(
        val filter: EarnFilterType,
        val option: EarnFilterTypeUM,
        val analytic: FilterTypeAnalytic,
    ) {
        override fun toString(): String =
            "GIVEN the $option option WHEN it is picked THEN $filter is applied and $analytic is reported"
    }
}