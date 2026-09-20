package com.tangem.features.tangempay.eligibility.country

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
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
    private val onDismiss: () -> Unit = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(getCountriesUseCase, getRegionsUseCase, onSelected, onDismiss)
        coEvery { getCountriesUseCase() } returns listOf(UNITED_STATES, GERMANY).right()
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

    private fun createModel(
        shouldIncludeRegions: Boolean,
        testScope: TestScope,
    ): CountrySelectorModel = CountrySelectorModel(
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        getCountriesUseCase = getCountriesUseCase,
        getRegionsUseCase = getRegionsUseCase,
        paramsContainer = MutableParamsContainer(
            Params.Countries(
                onDismiss = onDismiss,
                onSelected = onSelected,
                shouldIncludeRegions = shouldIncludeRegions,
            ),
        ),
    )

    private fun CountrySelectorModel.item(id: String): ResidencyListUM.ItemUM = items().first { it.id == id }

    private fun CountrySelectorModel.itemIds(): List<String> = items().map(ResidencyListUM.ItemUM::id)

    private fun CountrySelectorModel.items(): List<ResidencyListUM.ItemUM> {
        val content = uiState.value.content as ResidencyListUM.Content.Sections
        return content.sections.flatMap(ResidencyListUM.SectionUM::items)
    }

    private companion object {
        val UNITED_STATES = TangemPayCountry(
            code = "US",
            name = "United States",
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
        val CALIFORNIA = TangemPayRegion(code = "CA", name = "California", isSuggested = false)
    }
}