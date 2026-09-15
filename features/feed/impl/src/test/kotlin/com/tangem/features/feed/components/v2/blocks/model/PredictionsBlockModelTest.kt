package com.tangem.features.feed.components.v2.blocks.model

import com.google.common.truth.Truth.assertThat
import com.tangem.features.feed.components.v2.createTestingCoroutineDispatcherProvider
import com.tangem.features.polymarket.api.PolymarketFeatureToggles
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PredictionsBlockModelTest {

    private val featureToggles: PolymarketFeatureToggles = mockk()

    @ParameterizedTest
    @ProvideTestModels
    fun isVisible(model: VisibilityModel) = runTest {
        // Arrange
        every { featureToggles.isPolymarketEnabled } returns model.isFeatureEnabled

        // Act
        val blockModel = createModel(testScope = this)

        // Assert
        assertThat(blockModel.isVisible.value).isEqualTo(model.expected)
        blockModel.onDestroy()
    }

    internal data class VisibilityModel(val isFeatureEnabled: Boolean, val expected: Boolean)

    private fun provideTestModels() = listOf(
        VisibilityModel(isFeatureEnabled = true, expected = true),
        VisibilityModel(isFeatureEnabled = false, expected = false),
    )

    private fun createModel(testScope: TestScope): PredictionsBlockModel = PredictionsBlockModel(
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        featureToggles = featureToggles,
    )
}