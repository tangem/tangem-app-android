package com.tangem.feature.swap.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.collectibles.vault.GachaVaultError
import com.tangem.domain.transaction.error.SolanaGaslessError
import com.tangem.feature.swap.domain.models.ui.SwapTransactionState
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CollectiblesSwapErrorConverterTest {

    @ParameterizedTest
    @ProvideTestModels
    fun convert(model: ConvertModel) {
        // Act
        val actual = CollectiblesSwapErrorConverter.convert(model.error)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    data class ConvertModel(
        val error: CollectiblesAccountError,
        val expected: SwapTransactionState.Error?,
    )

    private fun provideTestModels() = listOf(
        ConvertModel(error = CollectiblesAccountError.Cancelled, expected = null),
        ConvertModel(
            error = CollectiblesAccountError.MaybeSent(txHash = null),
            expected = SwapTransactionState.Error.SolanaGaslessError(SolanaGaslessError.MaybeSent(signature = null)),
        ),
        ConvertModel(
            error = CollectiblesAccountError.Vault(GachaVaultError.SponsorUnavailable),
            expected = SwapTransactionState.Error.SolanaGaslessError(
                SolanaGaslessError.ServiceUnavailable(code = null, isSponsorshipUnavailable = true),
            ),
        ),
        ConvertModel(
            error = CollectiblesAccountError.Vault(GachaVaultError.SponsorRateLimited),
            expected = SwapTransactionState.Error.SolanaGaslessError(SolanaGaslessError.RateLimited(false)),
        ),
        ConvertModel(
            error = CollectiblesAccountError.Vault(GachaVaultError.TransactionExpired),
            expected = SwapTransactionState.Error.SolanaGaslessError(SolanaGaslessError.BlockhashExpired),
        ),
        ConvertModel(
            error = CollectiblesAccountError.Unsupported,
            expected = SwapTransactionState.Error.UnknownError,
        ),
    )
}