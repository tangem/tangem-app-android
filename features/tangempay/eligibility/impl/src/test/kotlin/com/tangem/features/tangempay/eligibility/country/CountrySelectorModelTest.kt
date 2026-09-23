package com.tangem.features.tangempay.eligibility.country

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.pay.TangemPayCountry
import com.tangem.domain.models.pay.TangemPayRegion
import com.tangem.domain.pay.usecase.GetTangemPayCountriesUseCase
import com.tangem.domain.pay.usecase.GetTangemPayRegionsUseCase
import com.tangem.features.tangempay.eligibility.TangemPayCountrySelectorComponent.Params
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CountrySelectorModelTest {

    private val getCountriesUseCase: GetTangemPayCountriesUseCase = mockk()
    private val getRegionsUseCase: GetTangemPayRegionsUseCase = mockk()
    private val onSelected: (TangemPayCountry, TangemPayRegion?) -> Unit = mockk(relaxed = true)
    private val onRegionSelected: (TangemPayRegion) -> Unit = mockk(relaxed = true)
    private val onDismiss: () -> Unit = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(getCountriesUseCase, getRegionsUseCase, onSelected, onRegionSelected, onDismiss)
        coEvery { getCountriesUseCase() } returns listOf(UNITED_STATES, GERMANY, BRAZIL, AUSTRIA).right()
        coEvery { getRegionsUseCase(UNITED_STATES.code) } returns listOf(CALIFORNIA).right()
    }

    @Test
    fun `GIVEN regions excluded WHEN country with regions clicked THEN country reported without region`() = runTest {
        // Arrange
        val model = createModel(shouldIncludeRegions = false, testScope = this)
        advanceUntilIdle()

        // Act
        model.item(UNITED_STATES.code).onClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { onSelected(UNITED_STATES, null) }
        verify(exactly = 1) { onDismiss() }
        coVerify(exactly = 0) { getRegionsUseCase(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN regions included WHEN country with regions clicked THEN regions list shown`() = runTest {
        // Arrange
        val model = createModel(shouldIncludeRegions = true, testScope = this)
        advanceUntilIdle()

        // Act
        model.item(UNITED_STATES.code).onClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.itemIds()).containsExactly(CALIFORNIA.code)
        assertThat(model.uiState.value.onBackClick).isNotNull()
        coVerify(exactly = 1) { getRegionsUseCase(UNITED_STATES.code) }
        verify(exactly = 0) { onSelected(any(), any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN regions excluded WHEN country without regions clicked THEN country reported`() = runTest {
        // Arrange
        val model = createModel(shouldIncludeRegions = false, testScope = this)
        advanceUntilIdle()

        // Act
        model.item(GERMANY.code).onClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { onSelected(GERMANY, null) }
        verify(exactly = 1) { onDismiss() }
        model.onDestroy()
    }

    @Test
    fun `GIVEN suggested country WHEN countries loaded THEN one section without a title`() = runTest {
        // Act
        val model = createModel(shouldIncludeRegions = false, testScope = this)
        advanceUntilIdle()

        // Assert
        val sections = model.sections()
        assertThat(sections).hasSize(1)
        assertThat(sections.single().title).isNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN unsorted countries WHEN countries loaded THEN items sorted by name`() = runTest {
        // Act
        val model = createModel(shouldIncludeRegions = false, testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.itemIds())
            .containsExactly(AUSTRIA.code, BRAZIL.code, GERMANY.code, UNITED_STATES.code)
            .inOrder()
        model.onDestroy()
    }

    @Test
    fun `GIVEN unsorted regions WHEN regions loaded THEN items sorted by name`() = runTest {
        // Arrange
        coEvery { getRegionsUseCase(UNITED_STATES.code) } returns listOf(NEW_YORK, CALIFORNIA).right()
        val model = createModel(shouldIncludeRegions = true, testScope = this)
        advanceUntilIdle()

        // Act
        model.item(UNITED_STATES.code).onClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.itemIds()).containsExactly(CALIFORNIA.code, NEW_YORK.code).inOrder()
        model.onDestroy()
    }

    @Test
    fun `GIVEN countries step WHEN model created THEN address country title shown`() = runTest {
        // Act
        val model = createModel(shouldIncludeRegions = true, testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.title).isEqualTo(resourceReference(R.string.tangempay_order_data_country))
        model.onDestroy()
    }

    @Test
    fun `GIVEN country with regions WHEN regions shown THEN address region title shown`() = runTest {
        // Arrange
        val model = createModel(shouldIncludeRegions = true, testScope = this)
        advanceUntilIdle()

        // Act
        model.item(UNITED_STATES.code).onClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.title).isEqualTo(resourceReference(R.string.tangempay_order_data_region))
        model.onDestroy()
    }

    @Test
    fun `GIVEN regions params WHEN model created THEN region title shown`() = runTest {
        // Act
        val model = createModel(
            params = Params.Regions(
                countryCode = UNITED_STATES.code,
                onDismiss = onDismiss,
                onSelected = onRegionSelected,
            ),
            testScope = this,
        )
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.title).isEqualTo(resourceReference(R.string.tangempay_order_data_region))
        model.onDestroy()
    }

    private fun createModel(shouldIncludeRegions: Boolean, testScope: TestScope): CountrySelectorModel = createModel(
        params = Params.Countries(
            onDismiss = onDismiss,
            onSelected = onSelected,
            shouldIncludeRegions = shouldIncludeRegions,
        ),
        testScope = testScope,
    )

    private fun createModel(params: Params, testScope: TestScope): CountrySelectorModel = CountrySelectorModel(
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        getCountriesUseCase = getCountriesUseCase,
        getRegionsUseCase = getRegionsUseCase,
        paramsContainer = MutableParamsContainer(params),
    )

    private fun CountrySelectorModel.item(id: String): ResidencyListUM.ItemUM = items().first { it.id == id }

    private fun CountrySelectorModel.itemIds(): List<String> = items().map(ResidencyListUM.ItemUM::id)

    private fun CountrySelectorModel.items(): List<ResidencyListUM.ItemUM> =
        sections().flatMap(ResidencyListUM.SectionUM::items)

    private fun CountrySelectorModel.sections(): List<ResidencyListUM.SectionUM> {
        val content = uiState.value.content as ResidencyListUM.Content.Sections
        return content.sections
    }

    private companion object {
        val UNITED_STATES = TangemPayCountry(
            code = "US",
            name = "United States",
            flagUrl = null,
            isSuggested = true,
            hasRegions = true,
        )
        val GERMANY = TangemPayCountry(
            code = "DE",
            name = "Germany",
            flagUrl = null,
            isSuggested = false,
            hasRegions = false,
        )
        val BRAZIL = TangemPayCountry(
            code = "BR",
            name = "Brazil",
            flagUrl = null,
            isSuggested = false,
            hasRegions = false,
        )
        val AUSTRIA = TangemPayCountry(
            code = "AT",
            name = "Austria",
            flagUrl = null,
            isSuggested = false,
            hasRegions = false,
        )
        val CALIFORNIA = TangemPayRegion(code = "CA", name = "California", isSuggested = false)
        val NEW_YORK = TangemPayRegion(code = "NY", name = "New York", isSuggested = true)
    }
}