package com.tangem.features.walletconnect.transaction.converter

import com.google.common.truth.Truth
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.walletconnect.model.WcPsbtOutput
import com.tangem.features.walletconnect.impl.R
import org.junit.jupiter.api.Test

class WcSignPsbtRequestInfoConverterTest {

    private val converter = WcSignPsbtRequestInfoConverter()

    @Test
    fun `GIVEN single output WHEN convert THEN one block with to and amount`() {
        val input = WcSignPsbtRequestInfoConverter.Input(
            outputs = listOf(WcPsbtOutput(address = "bc1qrecipient", amountSatoshi = 100_000L)),
            decimals = 8,
            symbol = "BTC",
        )

        val result = converter.convert(input)

        Truth.assertThat(result).hasSize(1)
        Truth.assertThat(result[0].info.map { it.title }).containsExactly(
            resourceReference(R.string.common_to),
            resourceReference(R.string.common_amount),
        ).inOrder()
        Truth.assertThat(result[0].info[0].description).isEqualTo("bc1qrecipient")
        Truth.assertThat(result[0].info[1].description).isEqualTo("0.001 BTC")
    }

    @Test
    fun `GIVEN multiple outputs WHEN convert THEN indexed blocks per output`() {
        val input = WcSignPsbtRequestInfoConverter.Input(
            outputs = listOf(
                WcPsbtOutput(address = "bc1qfirst", amountSatoshi = 250_000L),
                WcPsbtOutput(address = "bc1qsecond", amountSatoshi = 1_0000_0000L),
            ),
            decimals = 8,
            symbol = "BTC",
        )

        val result = converter.convert(input)

        Truth.assertThat(result).hasSize(2)
        Truth.assertThat(result[0].info[0].title)
            .isEqualTo(combinedReference(resourceReference(R.string.common_to), stringReference(" 1")))
        Truth.assertThat(result[0].info[1].description).isEqualTo("0.0025 BTC")
        Truth.assertThat(result[1].info[0].title)
            .isEqualTo(combinedReference(resourceReference(R.string.common_to), stringReference(" 2")))
        Truth.assertThat(result[1].info[0].description).isEqualTo("bc1qsecond")
        Truth.assertThat(result[1].info[1].description).isEqualTo("1 BTC")
    }

    @Test
    fun `GIVEN output with undecodable address WHEN convert THEN no address title and empty recipient`() {
        val input = WcSignPsbtRequestInfoConverter.Input(
            outputs = listOf(WcPsbtOutput(address = null, amountSatoshi = 50_000L)),
            decimals = 8,
            symbol = "BTC",
        )

        val result = converter.convert(input)

        Truth.assertThat(result[0].info[0].title).isEqualTo(resourceReference(R.string.common_no_address))
        Truth.assertThat(result[0].info[0].description).isEqualTo("")
        Truth.assertThat(result[0].info[1].description).isEqualTo("0.0005 BTC")
    }

    @Test
    fun `GIVEN no outputs WHEN convert THEN empty list`() {
        val input = WcSignPsbtRequestInfoConverter.Input(
            outputs = emptyList(),
            decimals = 8,
            symbol = "BTC",
        )

        Truth.assertThat(converter.convert(input)).isEmpty()
    }
}