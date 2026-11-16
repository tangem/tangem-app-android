package com.tangem.feature.swap.converters

import androidx.annotation.StringRes
import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.R
import com.tangem.core.ui.R as CoreUiR
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.transaction.error.SolanaGaslessError
import com.tangem.feature.swap.domain.models.ui.SwapTransactionState
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
 * Covers [SwapTransactionErrorStateConverter] mapping each [SolanaGaslessError] variant to its message.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapTransactionErrorStateConverterSolanaTest {

    private var dismissCount = 0
    private val converter = SwapTransactionErrorStateConverter(
        providerName = null,
        onDismiss = { dismissCount++ },
        onSupportClick = {},
    )

    @BeforeEach
    fun resetDismissCount() {
        dismissCount = 0
    }

    @Test
    fun `GIVEN gasless error WHEN convert THEN OK button dismisses the dialog`() {
        // Arrange
        val dialog = requireNotNull(
            converter.convert(SwapTransactionState.Error.SolanaGaslessError(SolanaGaslessError.Disabled)),
        )

        // Act
        dialog.firstAction.onClick()

        // Assert
        assertThat(dialog.firstAction.title).isEqualTo(resourceReference(CoreUiR.string.common_ok))
        assertThat(dismissCount).isEqualTo(1)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun convert(model: ErrorModel) {
        // Act
        val dialog = converter.convert(SwapTransactionState.Error.SolanaGaslessError(model.error))

        // Assert
        assertThat(dialog?.message).isEqualTo(resourceReference(model.expectedMessage))
    }

    private fun provideTestModels() = listOf(
        ErrorModel(
            error = SolanaGaslessError.BlockhashExpired,
            expectedMessage = R.string.gasless_solana_error_expired,
        ),
        ErrorModel(
            error = SolanaGaslessError.RateLimited(isPerWallet = true),
            expectedMessage = R.string.gasless_solana_error_rate_limited,
        ),
        ErrorModel(
            error = SolanaGaslessError.RateLimited(isPerWallet = false),
            expectedMessage = R.string.gasless_solana_error_rate_limited,
        ),
        ErrorModel(
            error = SolanaGaslessError.MaybeSent(signature = "sig"),
            expectedMessage = R.string.gasless_solana_error_maybe_sent,
        ),
        ErrorModel(
            error = SolanaGaslessError.MaybeSent(signature = null),
            expectedMessage = R.string.gasless_solana_error_maybe_sent,
        ),
        ErrorModel(
            error = SolanaGaslessError.Rejected(code = "413"),
            expectedMessage = R.string.gasless_solana_error_rejected,
        ),
        ErrorModel(
            error = SolanaGaslessError.Unauthorized,
            expectedMessage = R.string.gasless_solana_error_unavailable,
        ),
        ErrorModel(
            error = SolanaGaslessError.Disabled,
            expectedMessage = R.string.gasless_solana_error_unavailable,
        ),
        ErrorModel(
            error = SolanaGaslessError.ServiceUnavailable(code = "503"),
            expectedMessage = R.string.gasless_solana_error_unavailable,
        ),
        ErrorModel(
            error = SolanaGaslessError.Unknown(message = "boom"),
            expectedMessage = R.string.gasless_solana_error_unavailable,
        ),
    )

    internal data class ErrorModel(val error: SolanaGaslessError, @StringRes val expectedMessage: Int)
}