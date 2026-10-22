package com.tangem.domain.wallets.models

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ReferralParamValidatorTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN referral param WHEN sanitize THEN returns trimmed value or null`(model: SanitizeModel) {
        // Act
        val actual = ReferralParamValidator.sanitize(model.value)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        SanitizeModel(value = null, expected = null),
        SanitizeModel(value = "", expected = null),
        SanitizeModel(value = " ", expected = null),
        SanitizeModel(value = "null", expected = null),
        SanitizeModel(value = "NULL", expected = null),
        SanitizeModel(value = " Null ", expected = null),
        SanitizeModel(value = "abc", expected = "abc"),
        SanitizeModel(value = " abc ", expected = "abc"),
        SanitizeModel(value = "nullable", expected = "nullable"),
    )

    data class SanitizeModel(val value: String?, val expected: String?)
}