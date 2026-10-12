package com.tangem.features.feed.components.v2.blocks.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.abtests.manager.ABTestsManager
import com.tangem.features.feed.components.v2.createTestingCoroutineDispatcherProvider
import com.tangem.features.polymarket.api.PolymarketFeatureToggles
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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

    private val featureToggles: PolymarketFeatureToggles = mockk()
    private val abTestsManager: ABTestsManager = mockk()

    @BeforeEach
    fun setUp() {
        clearMocks(featureToggles, abTestsManager)
    }

    @Test
    fun `GIVEN toggle disabled WHEN created THEN block is hidden and experiment is not read`() = runTest {
        // Arrange
        every { featureToggles.isPolymarketEnabled } returns false

        // Act
        val blockModel = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(blockModel.isVisible.value).isFalse()
        coVerify(exactly = 0) { abTestsManager.getValue(any(), any()) }
        blockModel.onDestroy()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN toggle enabled WHEN experiment variant resolved THEN block visibility follows it`(
        model: VariantModel,
    ) = runTest {
        // Arrange
        every { featureToggles.isPolymarketEnabled } returns true
        coEvery { abTestsManager.getValue(key = "twi_1576_polymarket", defaultValue = "off") } returns model.variant

        // Act
        val blockModel = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(blockModel.isVisible.value).isEqualTo(model.expected)
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
        featureToggles = featureToggles,
        abTestsManager = abTestsManager,
    )
}