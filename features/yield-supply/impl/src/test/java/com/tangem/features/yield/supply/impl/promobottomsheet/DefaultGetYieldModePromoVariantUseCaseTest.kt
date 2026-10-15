package com.tangem.features.yield.supply.impl.promobottomsheet

import com.google.common.truth.Truth.assertThat
import com.tangem.core.abtests.manager.ABTestsManager
import com.tangem.features.yield.supply.api.YieldSupplyFeatureToggles
import com.tangem.features.yield.supply.api.promobottomsheet.YieldModePromoVariant
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultGetYieldModePromoVariantUseCaseTest {

    private val featureToggles: YieldSupplyFeatureToggles = mockk()
    private val abTestsManager: ABTestsManager = mockk()

    private val useCase = DefaultGetYieldModePromoVariantUseCase(
        featureToggles = featureToggles,
        abTestsManager = abTestsManager,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(featureToggles, abTestsManager)
        every { featureToggles.isYieldModePromoEnabled } returns true
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN an assigned value WHEN invoke THEN maps the exposed value to a variant`(model: ValueModel) = runTest {
        // Arrange
        every { abTestsManager.peekValue(KEY) } returns "control"
        coEvery { abTestsManager.getValue(KEY, "control") } returns model.value

        // Act
        val actual = useCase()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
        coVerify(exactly = 1) { abTestsManager.getValue(KEY, "control") }
    }

    @Test
    fun `GIVEN the release gate is closed WHEN invoke THEN control without reading the experiment`() = runTest {
        // Arrange
        every { featureToggles.isYieldModePromoEnabled } returns false

        // Act
        val actual = useCase()

        // Assert
        assertThat(actual).isEqualTo(YieldModePromoVariant.CONTROL)
        verify(exactly = 0) { abTestsManager.peekValue(any()) }
        coVerify(exactly = 0) { abTestsManager.getValue(any(), any()) }
    }

    @Test
    fun `GIVEN no variant value on the device WHEN invoke THEN control without an exposure`() = runTest {
        // Arrange
        every { abTestsManager.peekValue(KEY) } returns null

        // Act
        val actual = useCase()

        // Assert
        assertThat(actual).isEqualTo(YieldModePromoVariant.CONTROL)
        coVerify(exactly = 0) { abTestsManager.getValue(any(), any()) }
    }

    private fun provideTestModels() = listOf(
        ValueModel(value = "user_max_apy", expected = YieldModePromoVariant.USER_MAX_APY),
        ValueModel(value = "global_max_apy", expected = YieldModePromoVariant.GLOBAL_MAX_APY),
        ValueModel(value = "control", expected = YieldModePromoVariant.CONTROL),
        ValueModel(value = "off", expected = YieldModePromoVariant.CONTROL),
        ValueModel(value = "User_Max_Apy", expected = YieldModePromoVariant.CONTROL),
    )

    internal data class ValueModel(val value: String, val expected: YieldModePromoVariant)

    private companion object {
        const val KEY = "twi_1762_yield_mode_promo"
    }
}