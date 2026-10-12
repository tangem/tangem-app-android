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
    fun `GIVEN country with regions WHEN country clicked THEN regions list shown`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
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
    fun `GIVEN country with regions WHEN region clicked THEN country and region reported once`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.item(UNITED_STATES.code).onClick()
        advanceUntilIdle()

        // Act
        model.item(CALIFORNIA.code).onClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { onSelected(UNITED_STATES, CALIFORNIA) }
        verify(exactly = 1) { onDismiss() }
        model.onDestroy()
    }

    @Test
    fun `GIVEN country without regions WHEN country clicked THEN country reported without region`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.item(GERMANY.code).onClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { onSelected(GERMANY, null) }
        verify(exactly = 1) { onDismiss() }
        coVerify(exactly = 0) { getRegionsUseCase(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN no selection WHEN countries loaded THEN one section without a title`() = runTest {
        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        val sections = model.sections()
        assertThat(sections).hasSize(1)
        assertThat(sections.single().title).isNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN selected country WHEN countries loaded THEN selected section precedes the titled full list`() =
        runTest {
            // Act
            val model = createModel(selectedCountryCode = UNITED_STATES.code, testScope = this)
            advanceUntilIdle()

            // Assert
            val sections = model.sections()
            assertThat(sections.map { it.title }).containsExactly(
                resourceReference(R.string.tangempay_withdraw_bank_geo_country_selected),
                resourceReference(R.string.tangempay_residency_all_country),
            ).inOrder()
            assertThat(sections.first().items.map(ResidencyListUM.ItemUM::id)).containsExactly(UNITED_STATES.code)
            assertThat(sections.last().items.map(ResidencyListUM.ItemUM::id))
                .containsExactly(AUSTRIA.code, BRAZIL.code, GERMANY.code, UNITED_STATES.code)
                .inOrder()
            model.onDestroy()
        }

    @Test
    fun `GIVEN selected country WHEN search query typed THEN one section without a title`() = runTest {
        // Arrange
        val model = createModel(selectedCountryCode = UNITED_STATES.code, testScope = this)
        advanceUntilIdle()

        // Act
        model.uiState.value.search.onQueryChange("united")
        advanceUntilIdle()

        // Assert
        val sections = model.sections()
        assertThat(sections).hasSize(1)
        assertThat(sections.single().title).isNull()
        assertThat(sections.single().items.map(ResidencyListUM.ItemUM::id)).containsExactly(UNITED_STATES.code)
        model.onDestroy()
    }

    @Test
    fun `GIVEN selected region of the picked country WHEN regions shown THEN selected section titled`() = runTest {
        // Arrange
        coEvery { getRegionsUseCase(UNITED_STATES.code) } returns listOf(NEW_YORK, CALIFORNIA).right()
        val model = createModel(
            selectedCountryCode = UNITED_STATES.code,
            selectedRegionCode = CALIFORNIA.code,
            testScope = this,
        )
        advanceUntilIdle()

        // Act
        model.item(UNITED_STATES.code).onClick()
        advanceUntilIdle()

        // Assert
        val sections = model.sections()
        assertThat(sections.map { it.title }).containsExactly(
            resourceReference(R.string.tangempay_withdraw_bank_geo_state_selected),
            resourceReference(R.string.tangempay_residency_all_state),
        ).inOrder()
        assertThat(sections.first().items.map(ResidencyListUM.ItemUM::id)).containsExactly(CALIFORNIA.code)
        model.onDestroy()
    }

    @Test
    fun `GIVEN selected region of another country WHEN regions shown THEN nothing highlighted`() = runTest {
        // Arrange
        coEvery { getCountriesUseCase() } returns listOf(UNITED_STATES, GREAT_BRITAIN).right()
        coEvery { getRegionsUseCase(GREAT_BRITAIN.code) } returns listOf(CALIFORNIA).right()
        val model = createModel(
            selectedCountryCode = UNITED_STATES.code,
            selectedRegionCode = CALIFORNIA.code,
            testScope = this,
        )
        advanceUntilIdle()

        // Act
        model.item(GREAT_BRITAIN.code).onClick()
        advanceUntilIdle()

        // Assert
        val sections = model.sections()
        assertThat(sections).hasSize(1)
        assertThat(sections.single().title).isNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN bare selected region code WHEN iso coded regions loaded THEN subdivision matched`() = runTest {
        // Act
        val model = createModel(
            params = Params.Regions(
                countryCode = UNITED_STATES.code,
                onDismiss = onDismiss,
                onSelected = onRegionSelected,
                selectedRegionCode = CALIFORNIA.code,
            ),
            testScope = this,
            regions = listOf(CALIFORNIA_ISO, NEW_YORK_ISO),
        )
        advanceUntilIdle()

        // Assert
        val sections = model.sections()
        assertThat(sections.first().items.map(ResidencyListUM.ItemUM::id)).containsExactly(CALIFORNIA_ISO.code)
        assertThat(sections.first().title)
            .isEqualTo(resourceReference(R.string.tangempay_withdraw_bank_geo_state_selected))
        model.onDestroy()
    }

    @Test
    fun `GIVEN unsorted countries WHEN countries loaded THEN items sorted by name`() = runTest {
        // Act
        val model = createModel(testScope = this)
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
        val model = createModel(testScope = this)
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
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.title).isEqualTo(resourceReference(R.string.tangempay_order_data_country))
        model.onDestroy()
    }

    @Test
    fun `GIVEN country with regions WHEN regions shown THEN state title shown`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.item(UNITED_STATES.code).onClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.title)
            .isEqualTo(resourceReference(R.string.tangempay_withdraw_bank_geo_state_title))
        model.onDestroy()
    }

    @Test
    fun `GIVEN regions params WHEN model created THEN state title shown`() = runTest {
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
        assertThat(model.uiState.value.title)
            .isEqualTo(resourceReference(R.string.tangempay_withdraw_bank_geo_state_title))
        model.onDestroy()
    }

    private fun createModel(
        testScope: TestScope,
        selectedCountryCode: String? = null,
        selectedRegionCode: String? = null,
    ): CountrySelectorModel = createModel(
        params = Params.Countries(
            onDismiss = onDismiss,
            onSelected = onSelected,
            selectedCountryCode = selectedCountryCode,
            selectedRegionCode = selectedRegionCode,
        ),
        testScope = testScope,
    )

    private fun createModel(
        params: Params,
        testScope: TestScope,
        regions: List<TangemPayRegion>? = null,
    ): CountrySelectorModel {
        if (regions != null) {
            coEvery { getRegionsUseCase(any()) } returns regions.right()
        }
        return CountrySelectorModel(
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            getCountriesUseCase = getCountriesUseCase,
            getRegionsUseCase = getRegionsUseCase,
            paramsContainer = MutableParamsContainer(params),
        )
    }

    private fun CountrySelectorModel.item(id: String): ResidencyListUM.ItemUM = items().first { it.id == id }

    private fun CountrySelectorModel.itemIds(): List<String> = items().map(ResidencyListUM.ItemUM::id)

    private fun CountrySelectorModel.items(): List<ResidencyListUM.ItemUM> = sections().last().items

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
        val GREAT_BRITAIN = TangemPayCountry(
            code = "GB",
            name = "United Kingdom",
            flagUrl = null,
            isSuggested = false,
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
        val CALIFORNIA_ISO = TangemPayRegion(code = "US-CA", name = "California", isSuggested = false)
        val NEW_YORK_ISO = TangemPayRegion(code = "US-NY", name = "New York", isSuggested = true)
    }
}