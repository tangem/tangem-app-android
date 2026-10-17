package com.tangem.features.txhistory.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.express.models.ExpressExchangeStatus
import com.tangem.domain.express.models.ExpressOnrampStatus
import com.tangem.features.txhistory.impl.R
import org.junit.jupiter.api.Test

/**
 * The shared `ID:` line carries the same id the header menu copies: the provider-side one, falling back to the
 * Tangem-side deal id when the provider supplies none (a DEX swap).
 */
internal class ExpressTxToShareTextConverterTest : TxDetailsConverterTestBase() {

    private val converter = ExpressTxToShareTextConverter()

    @Test
    fun `GIVEN swap with provider id WHEN convert THEN shares provider id`() {
        // Arrange
        val swap = expressSwap(status = ExpressExchangeStatus.Finished, externalTxId = PROVIDER_SWAP_ID)

        // Act
        val text = converter.convert(swap)

        // Assert
        assertThat(text.idLine()).isEqualTo(expectedIdLine(PROVIDER_SWAP_ID))
    }

    @Test
    fun `GIVEN swap without provider id WHEN convert THEN shares express deal id`() {
        // Arrange
        val swap = expressSwap(status = ExpressExchangeStatus.Finished, externalTxId = null)

        // Act
        val text = converter.convert(swap)

        // Assert
        assertThat(text.idLine()).isEqualTo(expectedIdLine(swap.txId))
    }

    @Test
    fun `GIVEN onramp with provider id WHEN convert THEN shares provider id`() {
        // Arrange
        val onramp = expressOnramp(status = ExpressOnrampStatus.Finished, externalTxId = PROVIDER_ONRAMP_ID)

        // Act
        val text = converter.convert(onramp)

        // Assert
        assertThat(text.idLine()).isEqualTo(expectedIdLine(PROVIDER_ONRAMP_ID))
    }

    @Test
    fun `GIVEN onramp without provider id WHEN convert THEN shares express deal id`() {
        // Arrange
        val onramp = expressOnramp(status = ExpressOnrampStatus.Finished, externalTxId = null)

        // Act
        val text = converter.convert(onramp)

        // Assert
        assertThat(text.idLine()).isEqualTo(expectedIdLine(onramp.txId))
    }

    /** The single `ID: …` line of the shared block, picked out of the joined lines. */
    private fun TextReference.idLine(): TextReference? = (this as TextReference.Combined).refs.data
        .filterIsInstance<TextReference.Res>()
        .singleOrNull { it.id == R.string.express_transaction_id }

    private fun expectedIdLine(id: String): TextReference =
        resourceReference(R.string.express_transaction_id, wrappedList(id))

    private companion object {
        const val PROVIDER_SWAP_ID = "gmb2nytjuxffycnawn"
        const val PROVIDER_ONRAMP_ID = "4f0d34df-70c0-4e8d-9684-76f9c4e2e5d9"
    }
}