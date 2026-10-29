package com.tangem.features.storiesv2.impl.domain

import com.google.common.truth.Truth.assertThat
import com.tangem.core.abtests.manager.ABTestsManager
import com.tangem.features.storiesv2.HardwareUpgradeStoriesVariant
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultGetHardwareUpgradeStoriesVariantUseCaseTest {

    private val abTestsManager: ABTestsManager = mockk()

    private val useCase = DefaultGetHardwareUpgradeStoriesVariantUseCase(
        abTestsManager = abTestsManager,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(abTestsManager)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN an experiment value WHEN invoke THEN only treatment shows the story`(model: ValueModel) = runTest {
        // Arrange
        coEvery { abTestsManager.getValue(KEY, "control") } returns model.value

        // Act
        val actual = useCase()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @ParameterizedTest
    @MethodSource("providePeekModels")
    fun `GIVEN a value on the device WHEN peek THEN only treatment shows the story`(model: PeekModel) {
        // Arrange
        every { abTestsManager.peekValue(KEY) } returns model.value

        // Act
        val actual = useCase.peek()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun providePeekModels() = listOf(
        PeekModel(value = "treatment", expected = HardwareUpgradeStoriesVariant.TREATMENT),
        PeekModel(value = "control", expected = HardwareUpgradeStoriesVariant.CONTROL),
        PeekModel(value = null, expected = HardwareUpgradeStoriesVariant.CONTROL),
    )

    internal data class PeekModel(val value: String?, val expected: HardwareUpgradeStoriesVariant)

    private fun provideTestModels() = listOf(
        ValueModel(value = "treatment", expected = HardwareUpgradeStoriesVariant.TREATMENT),
        ValueModel(value = "control", expected = HardwareUpgradeStoriesVariant.CONTROL),
        ValueModel(value = "on", expected = HardwareUpgradeStoriesVariant.CONTROL),
        ValueModel(value = "Treatment", expected = HardwareUpgradeStoriesVariant.CONTROL),
    )

    internal data class ValueModel(val value: String, val expected: HardwareUpgradeStoriesVariant)

    private companion object {
        const val KEY = "twi_1357_main_hardware_upgrade_stories"
    }
}