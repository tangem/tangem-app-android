package com.tangem.features.tokenreceive.experiment

import com.google.common.truth.Truth.assertThat
import com.tangem.core.abtests.manager.ABTestsManager
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetAddFundsUpdateVariantUseCaseTest {

    private val abTestsManager: ABTestsManager = mockk()
    private val useCase = GetAddFundsUpdateVariantUseCase(abTestsManager)

    @BeforeEach
    fun resetMocks() {
        clearMocks(abTestsManager)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN cached value WHEN invoke THEN only treatment shows the update`(model: VariantModel) {
        // Arrange
        every { abTestsManager.getCachedValue("twi_1958_add_funds_update") } returns model.cachedValue

        // Act
        val actual = useCase()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        VariantModel(cachedValue = "treatment", expected = AddFundsUpdateVariant.TREATMENT),
        VariantModel(cachedValue = "control", expected = AddFundsUpdateVariant.CONTROL),
        VariantModel(cachedValue = "unknown", expected = AddFundsUpdateVariant.CONTROL),
        VariantModel(cachedValue = null, expected = AddFundsUpdateVariant.CONTROL),
    )

    data class VariantModel(val cachedValue: String?, val expected: AddFundsUpdateVariant)
}