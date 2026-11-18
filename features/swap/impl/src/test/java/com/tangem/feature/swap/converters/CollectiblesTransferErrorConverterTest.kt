package com.tangem.feature.swap.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.R
import com.tangem.core.ui.R as CoreUiR
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.collectibles.vault.GachaVaultError
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CollectiblesTransferErrorConverterTest {

    private var dismissCount = 0
    private val converter = CollectiblesTransferErrorConverter(onDismiss = { dismissCount++ })

    @BeforeEach
    fun resetDismissCount() {
        dismissCount = 0
    }

    @Test
    fun `GIVEN the user cancelled WHEN convert THEN no alert is shown`() {
        // Act
        val dialog = converter.convert(CollectiblesAccountError.Cancelled)

        // Assert
        assertThat(dialog).isNull()
    }

    @Test
    fun `GIVEN a sponsor error WHEN its alert is confirmed THEN it is dismissed`() {
        // Arrange
        val dialog = requireNotNull(converter.convert(vault(GachaVaultError.SponsorUnavailable)))

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
        val dialog = converter.convert(model.error)

        // Assert
        assertThat(dialog?.message).isEqualTo(model.expectedMessage)
    }

    private fun provideTestModels() = listOf(
        ErrorModel(
            error = CollectiblesAccountError.MaybeSent(txHash = null),
            expectedMessage = resourceReference(R.string.collectibles_withdraw_transfer_error_maybe_sent),
        ),
        ErrorModel(
            error = vault(GachaVaultError.SponsorUnavailable),
            expectedMessage = resourceReference(R.string.gasless_solana_error_unavailable),
        ),
        ErrorModel(
            error = vault(GachaVaultError.SponsorRateLimited),
            expectedMessage = resourceReference(R.string.collectibles_withdraw_transfer_error_rate_limited),
        ),
        ErrorModel(
            error = vault(GachaVaultError.TransactionExpired),
            expectedMessage = resourceReference(R.string.collectibles_withdraw_transfer_error_expired),
        ),
        ErrorModel(
            error = vault(GachaVaultError.SponsorRejected(reason = "SOLANA_SIMULATION_FAILED")),
            expectedMessage = resourceReference(CoreUiR.string.common_unknown_error),
        ),
        ErrorModel(
            error = CollectiblesAccountError.SessionKeyNotProvisioned,
            expectedMessage = resourceReference(CoreUiR.string.common_unknown_error),
        ),
    )

    private fun vault(error: GachaVaultError) = CollectiblesAccountError.Vault(error)

    data class ErrorModel(
        val error: CollectiblesAccountError,
        val expectedMessage: TextReference,
    )
}