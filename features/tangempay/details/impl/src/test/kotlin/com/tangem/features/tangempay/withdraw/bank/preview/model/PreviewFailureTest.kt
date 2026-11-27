package com.tangem.features.tangempay.withdraw.bank.preview.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpError
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PreviewFailureTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN bank off-ramp error WHEN toFailure THEN region refusal gets its screen and the rest the general one`(
        model: FailureModel,
    ) {
        // Act
        val actual = model.error.toFailure()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        FailureModel(error = BankOfframpError.SenderRegionUnsupported, expected = PreviewFailure.SenderRegion),
        FailureModel(
            error = BankOfframpError.Unavailable(errorCode = 104120100),
            expected = PreviewFailure.UnsupportedConfiguration,
        ),
        FailureModel(error = BankOfframpError.QuoteExpired, expected = PreviewFailure.General),
        FailureModel(error = BankOfframpError.VerificationIncomplete, expected = PreviewFailure.General),
        FailureModel(error = BankOfframpError.Conflict, expected = PreviewFailure.General),
        FailureModel(error = BankOfframpError.AmountOutOfLimits, expected = PreviewFailure.General),
        FailureModel(error = BankOfframpError.Rejected, expected = PreviewFailure.General),
        FailureModel(error = BankOfframpError.BadRequest, expected = PreviewFailure.General),
    )

    data class FailureModel(val error: BankOfframpError, val expected: PreviewFailure)
}