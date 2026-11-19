package com.tangem.features.feed.earn.model.filters

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.earn.model.EarnFilterType
import com.tangem.features.earn.api.EarnFeatureToggles
import com.tangem.features.feed.earn.components.EarnTypeFilterComponent
import com.tangem.features.feed.earn.model.analytics.EarnAnalyticsEvent
import com.tangem.features.feed.earn.model.analytics.FilterTypeAnalytic
import com.tangem.features.feed.earn.ui.state.EarnFilterTypeUM
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnTypeFilterModelTest {

    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val earnFeatureToggles: EarnFeatureToggles = mockk()

    private var selectedFilter: EarnFilterType? = null
    private val appliedType = MutableStateFlow(EarnFilterType.ALL)

    @BeforeEach
    fun reset() {
        clearMocks(analyticsEventHandler, earnFeatureToggles)
        every { earnFeatureToggles.isEarnEnabled } returns true
        selectedFilter = null
    }

    private fun createModel(selected: EarnFilterType = EarnFilterType.ALL): EarnTypeFilterModel {
        appliedType.value = selected
        return EarnTypeFilterModel(
            dispatchers = TestingCoroutineDispatcherProvider(),
            analyticsEventHandler = analyticsEventHandler,
            earnFeatureToggles = earnFeatureToggles,
            paramsContainer = MutableParamsContainer(
                EarnTypeFilterComponent.Params(
                    selectedFilter = appliedType,
                    onFilterSelected = { selectedFilter = it },
                    onDismiss = {},
                ),
            ),
        )
    }

    @Test
    fun `GIVEN an open sheet WHEN the applied type changes THEN the tick follows it`() {
        // Arrange
        val underTest = createModel(selected = EarnFilterType.STAKING)

        // Act
        appliedType.value = EarnFilterType.YIELD

        // Assert
        assertThat(underTest.state.value.selectedOption).isEqualTo(EarnFilterTypeUM.YieldMode)
        assertThat(selectedFilter).isNull()
    }

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
        OptionModel(
            name = "GIVEN vaults WHEN the sheet is used THEN the vaults option is bound to the VAULT filter",
            filter = EarnFilterType.VAULT,
            option = EarnFilterTypeUM.Vaults,
            analytic = FilterTypeAnalytic.VAULTS,
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