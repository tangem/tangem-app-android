package com.tangem.feature.swap.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.transaction.error.SolanaGaslessError
import com.tangem.feature.swap.domain.models.ui.SwapTransactionState
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SolanaGaslessRefusalTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN swap error WHEN isSolanaGaslessRefusal THEN matches error`(model: RefusalModel) {
        // Act
        val actual = model.error.isSolanaGaslessRefusal

        // Assert
        assertThat(actual).isEqualTo(model.isRefusal)
    }

    data class RefusalModel(val error: SwapTransactionState.Error, val isRefusal: Boolean)

    private fun provideTestModels() = listOf(
        RefusalModel(gasless(SolanaGaslessError.RateLimited(isPerWallet = false)), isRefusal = true),
        RefusalModel(gasless(SolanaGaslessError.Rejected(code = "SOLANA_SIMULATION_FAILED")), isRefusal = true),
        RefusalModel(gasless(SolanaGaslessError.BlockhashExpired), isRefusal = false),
        RefusalModel(gasless(SolanaGaslessError.ServiceUnavailable(code = null)), isRefusal = false),
        RefusalModel(
            gasless(
                SolanaGaslessError.ServiceUnavailable("SPONSOR_INSUFFICIENT_FUNDS", isSponsorshipUnavailable = true),
            ),
            isRefusal = true,
        ),
        RefusalModel(gasless(SolanaGaslessError.MaybeSent(signature = null)), isRefusal = true),
        RefusalModel(gasless(SolanaGaslessError.MaybeSent(signature = "sig")), isRefusal = true),
        RefusalModel(SwapTransactionState.Error.UnknownError, isRefusal = false),
    )

    private fun gasless(error: SolanaGaslessError) = SwapTransactionState.Error.SolanaGaslessError(error)
}