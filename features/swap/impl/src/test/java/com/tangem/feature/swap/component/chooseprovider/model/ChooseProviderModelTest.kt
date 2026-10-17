package com.tangem.feature.swap.component.chooseprovider.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.event.SwapAnalyticsEvent
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.express.models.ProviderFilterType
import com.tangem.feature.swap.component.chooseprovider.ChooseProviderComponent
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.models.states.ChooseProviderSheetSource
import com.tangem.feature.swap.models.states.PercentDifference
import com.tangem.feature.swap.models.states.ProviderState
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ChooseProviderModelTest {

    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxUnitFun = true)

    private val cexRow = providerRow(id = "changelly", type = ExchangeProviderType.CEX.providerName)
    private val dexRow = providerRow(id = "1inch", type = ExchangeProviderType.DEX.providerName)
    private val bridgeRow = providerRow(id = "rango", type = ExchangeProviderType.DEX_BRIDGE.providerName)

    @BeforeEach
    fun resetMocks() {
        clearMocks(analyticsEventHandler)
    }

    @Test
    fun `GIVEN cex and dex rows WHEN model created THEN all rows shown with ALL filter`() = runTest {
        // Arrange
        val source = MutableStateFlow(source(cexRow, dexRow))

        // Act
        val model = createModel(source)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.providers).containsExactly(cexRow, dexRow).inOrder()
        assertThat(model.uiState.value.selectedFilter).isEqualTo(ProviderFilterType.ALL)
        model.onDestroy()
    }

    @Test
    fun `GIVEN cex and dex rows WHEN DEX filter selected THEN only dex and bridge rows shown`() = runTest {
        // Arrange
        val model = createModel(MutableStateFlow(source(cexRow, dexRow, bridgeRow)))
        advanceUntilIdle()

        // Act
        model.onFilterSelect(ProviderFilterType.DEX)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.providers).containsExactly(dexRow, bridgeRow).inOrder()
        assertThat(model.uiState.value.selectedFilter).isEqualTo(ProviderFilterType.DEX)
        model.onDestroy()
    }

    @Test
    fun `GIVEN CEX filter selected WHEN source refreshed THEN filter kept and applied to new rows`() = runTest {
        // Arrange
        val source = MutableStateFlow(source(cexRow, dexRow))
        val model = createModel(source)
        advanceUntilIdle()
        model.onFilterSelect(ProviderFilterType.CEX)
        advanceUntilIdle()
        val refreshedCexRow = cexRow.copy(subtitle = stringReference("2 BTC"))

        // Act
        source.value = source(refreshedCexRow, dexRow)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.providers).containsExactly(refreshedCexRow)
        assertThat(model.uiState.value.allProviders).containsExactly(refreshedCexRow, dexRow).inOrder()
        assertThat(model.uiState.value.selectedFilter).isEqualTo(ProviderFilterType.CEX)
        model.onDestroy()
    }

    @Test
    fun `GIVEN rows WHEN filter selected THEN filter analytics sent`() = runTest {
        // Arrange
        val model = createModel(MutableStateFlow(source(cexRow, dexRow)))
        val event = slot<SwapAnalyticsEvent.FilterProvider>()

        // Act
        model.onFilterSelect(ProviderFilterType.CEX)

        // Assert
        verify(exactly = 1) { analyticsEventHandler.send(capture(event)) }
        assertThat(event.captured.params).isEqualTo(SwapAnalyticsEvent.FilterProvider(filterType = "CEX").params)
        model.onDestroy()
    }

    private fun TestScope.createModel(source: MutableStateFlow<ChooseProviderSheetSource>) = ChooseProviderModel(
        dispatchers = createTestingCoroutineDispatcherProvider(),
        analyticsEventHandler = analyticsEventHandler,
        paramsContainer = MutableParamsContainer(
            ChooseProviderComponent.Params(source = source, onCloseClick = {}),
        ),
    )

    private fun source(vararg rows: ProviderState) = ChooseProviderSheetSource(
        selectedProviderId = rows.first().id,
        allProviders = persistentListOf(*rows),
        notification = null,
        availableFilters = persistentListOf(ProviderFilterType.ALL, ProviderFilterType.CEX, ProviderFilterType.DEX),
    )

    private fun providerRow(id: String, type: String) = ProviderState.Content(
        id = id,
        name = id,
        type = type,
        iconUrl = "",
        subtitle = stringReference("1 BTC"),
        selectionType = ProviderState.SelectionType.SELECT,
        additionalBadge = ProviderState.AdditionalBadge.Empty,
        percentLowerThenBest = PercentDifference.Empty,
        namePrefix = ProviderState.PrefixType.NONE,
        onProviderClick = {},
    )
}