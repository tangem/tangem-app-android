package com.tangem.features.walletconnect.transaction.converter

import com.google.common.truth.Truth
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.walletconnect.model.WcBitcoinMethod
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestBlockUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestInfoItemUM
import kotlinx.collections.immutable.toImmutableList
import org.junit.jupiter.api.Test

class WcBtcSendTransferRequestInfoConverterTest {

    private val converter = WcBtcSendTransferRequestInfoConverter()

    @Test
    fun `GIVEN sendTransfer with change address WHEN convert THEN block with from to amount and change`() {
        val input = WcBtcSendTransferRequestInfoConverter.Input(
            method = WcBitcoinMethod.SendTransfer(
                account = "bc1qsenderaddress",
                recipientAddress = "bc1qrecipientaddress",
                amount = "5000000",
                memo = null,
                changeAddress = "bc1qchangeaddress",
            ),
            decimals = 8,
            symbol = "BTC",
        )

        val expected = WcTransactionRequestBlockUM(
            info = listOf(
                WcTransactionRequestInfoItemUM(resourceReference(R.string.common_from), "bc1qsenderaddress"),
                WcTransactionRequestInfoItemUM(resourceReference(R.string.common_to), "bc1qrecipientaddress"),
                WcTransactionRequestInfoItemUM(resourceReference(R.string.common_amount), "0.05 BTC"),
                WcTransactionRequestInfoItemUM(resourceReference(R.string.wc_change_address), "bc1qchangeaddress"),
            ).toImmutableList(),
        )

        Truth.assertThat(converter.convert(input)).isEqualTo(expected)
    }

    @Test
    fun `GIVEN sendTransfer without change address WHEN convert THEN block with from to amount only`() {
        val input = WcBtcSendTransferRequestInfoConverter.Input(
            method = WcBitcoinMethod.SendTransfer(
                account = "bc1qsenderaddress",
                recipientAddress = "bc1qrecipientaddress",
                amount = "100000000",
                memo = null,
                changeAddress = null,
            ),
            decimals = 8,
            symbol = "BTC",
        )

        val expected = WcTransactionRequestBlockUM(
            info = listOf(
                WcTransactionRequestInfoItemUM(resourceReference(R.string.common_from), "bc1qsenderaddress"),
                WcTransactionRequestInfoItemUM(resourceReference(R.string.common_to), "bc1qrecipientaddress"),
                WcTransactionRequestInfoItemUM(resourceReference(R.string.common_amount), "1 BTC"),
            ).toImmutableList(),
        )

        Truth.assertThat(converter.convert(input)).isEqualTo(expected)
    }

    @Test
    fun `GIVEN blank change address WHEN convert THEN change address item is omitted`() {
        val input = WcBtcSendTransferRequestInfoConverter.Input(
            method = WcBitcoinMethod.SendTransfer(
                account = "bc1qsenderaddress",
                recipientAddress = "bc1qrecipientaddress",
                amount = "1",
                memo = null,
                changeAddress = "",
            ),
            decimals = 8,
            symbol = "BTC",
        )

        val result = converter.convert(input)

        Truth.assertThat(result.info).hasSize(3)
        Truth.assertThat(result.info.last().title).isEqualTo(resourceReference(R.string.common_amount))
    }

    @Test
    fun `GIVEN non-numeric amount WHEN convert THEN raw amount is shown`() {
        val input = WcBtcSendTransferRequestInfoConverter.Input(
            method = WcBitcoinMethod.SendTransfer(
                account = "bc1qsenderaddress",
                recipientAddress = "bc1qrecipientaddress",
                amount = "not-a-number",
                memo = null,
                changeAddress = null,
            ),
            decimals = 8,
            symbol = "BTC",
        )

        val amountItem = converter.convert(input).info[2]

        Truth.assertThat(amountItem.title).isEqualTo(resourceReference(R.string.common_amount))
        Truth.assertThat(amountItem.description).isEqualTo("not-a-number")
    }
}