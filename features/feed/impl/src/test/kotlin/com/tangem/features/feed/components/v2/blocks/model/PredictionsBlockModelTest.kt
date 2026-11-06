package com.tangem.features.feed.components.v2.blocks.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.abtests.manager.ABTestsManager
import com.tangem.features.feed.components.v2.createTestingCoroutineDispatcherProvider
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PredictionsBlockModelTest {

    private val abTestsManager: ABTestsManager = mockk()

    @BeforeEach
    fun setUp() {
        clearMocks(abTestsManager)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN experiment variant resolved WHEN created THEN block visibility follows it`(
        model: VariantModel,
    ) = runTest {
        // Arrange
        coEvery { abTestsManager.getValue(key = "twi_1576_polymarket", defaultValue = "off") } returns model.variant

        // Act
        val blockModel = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(blockModel.isVisible.value).isEqualTo(model.expected)
        blockModel.onDestroy()
    }

    @Test
    fun `GIVEN variant off at creation WHEN visibility refreshed after variant turns on THEN block becomes visible`() =
        runTest {
            // Arrange
            coEvery { abTestsManager.getValue(key = "twi_1576_polymarket", defaultValue = "off") } returnsMany
                listOf("off", "on")
            val blockModel = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            blockModel.refreshVisibility()
            advanceUntilIdle()

            // Assert
            assertThat(blockModel.isVisible.value).isTrue()
            blockModel.onDestroy()
        }

    internal data class VariantModel(val variant: String, val expected: Boolean)

    private fun provideTestModels() = listOf(
        VariantModel(variant = "on", expected = true),
        VariantModel(variant = "off", expected = false),
        VariantModel(variant = "control", expected = false),
        VariantModel(variant = "ON", expected = false),
    )

    private fun createModel(testScope: TestScope): PredictionsBlockModel = PredictionsBlockModel(
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        abTestsManager = abTestsManager,
    )
}