package com.tangem.features.feed.earn.model.filters

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.earn.model.EarnFilterType
import com.tangem.features.feed.earn.components.EarnTypeFilterComponent
import com.tangem.features.feed.earn.model.analytics.EarnAnalyticsEvent
import com.tangem.features.feed.earn.model.analytics.FilterTypeAnalytic
import com.tangem.features.feed.earn.ui.state.EarnFilterTypeUM
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnTypeFilterModelTest {

    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)

    private var selectedFilter: EarnFilterType? = null

    @BeforeEach
    fun reset() {
        clearMocks(analyticsEventHandler)
        selectedFilter = null
    }

    private fun createModel(selected: EarnFilterType = EarnFilterType.ALL) = EarnTypeFilterModel(
        dispatchers = TestingCoroutineDispatcherProvider(),
        analyticsEventHandler = analyticsEventHandler,
        paramsContainer = MutableParamsContainer(
            EarnTypeFilterComponent.Params(
                selectedFilter = selected,
                onFilterSelected = { selectedFilter = it },
                onDismiss = {},
            ),
        ),
    )

    @ParameterizedTest
    @ProvideTestModels
    fun initialState(model: OptionModel) {
        // Act
        val actual = createModel(selected = model.filter)

        // Assert
        assertThat(actual.state.value.selectedOption).isEqualTo(model.option)
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

    @Test
    fun `GIVEN an option WHEN it is picked THEN the sheet keeps showing the applied filter`() {
        // The pick only leaves the sheet through the callback; the tab decides what is applied, and the
        // sheet is dismissed rather than re-rendered.
        // Arrange
        val underTest = createModel(selected = EarnFilterType.ALL)

        // Act
        underTest.state.value.onOptionClick(EarnFilterTypeUM.Staking)

        // Assert
        assertThat(underTest.state.value.selectedOption).isEqualTo(EarnFilterTypeUM.All)
    }

    private fun provideTestModels() = listOf(
        OptionModel(
            name = "GIVEN all types WHEN the sheet is used THEN the all option is bound to the ALL filter",
            filter = EarnFilterType.ALL,
            option = EarnFilterTypeUM.All,
            analytic = FilterTypeAnalytic.ALL_TYPES,
        ),
        OptionModel(
            name = "GIVEN staking WHEN the sheet is used THEN the staking option is bound to the STAKING filter",
            filter = EarnFilterType.STAKING,
            option = EarnFilterTypeUM.Staking,
            analytic = FilterTypeAnalytic.STAKING,
        ),
        OptionModel(
            name = "GIVEN yield WHEN the sheet is used THEN the yield mode option is bound to the YIELD filter",
            filter = EarnFilterType.YIELD,
            option = EarnFilterTypeUM.YieldMode,
            analytic = FilterTypeAnalytic.YIELD,
        ),
    )

    internal data class OptionModel(
        val name: String,
        val filter: EarnFilterType,
        val option: EarnFilterTypeUM,
        val analytic: FilterTypeAnalytic,
    ) {
        override fun toString(): String = name
    }
}