package com.tangem.data.blockaid

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyEnterWithNativeCallData
import com.tangem.store.datasource.blockaid.models.request.Data
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import com.tangem.blockchain.common.TransactionData as SDKTransactionData

internal class BlockAidEvmScanTransactionConverterTest {

    private val converter = BlockAidEvmScanTransactionConverter(Blockchain.Ethereum)

    @Test
    fun `GIVEN payable call with a coin amount WHEN convert THEN the request carries the value in wei`() {
        // Arrange
        val tx = transaction(amount = Amount(value = BigDecimal("0.5"), blockchain = Blockchain.Ethereum))

        // Act
        val actual = converter.convert(listOf(tx)).data.single()

        // Assert
        assertThat(actual).isEqualTo(
            Data(from = OWNER, to = MODULE, data = NATIVE_ENTER_DATA, value = "0x6f05b59d3b20000"),
        )
    }

    @Test
    fun `GIVEN zero coin amount WHEN convert THEN the request has no value`() {
        // Arrange
        val tx = transaction(amount = Amount(value = BigDecimal.ZERO, blockchain = Blockchain.Ethereum))

        // Act
        val actual = converter.convert(listOf(tx)).data.single()

        // Assert
        assertThat(actual).isEqualTo(Data(from = OWNER, to = MODULE, data = NATIVE_ENTER_DATA))
    }

    @Test
    fun `GIVEN token amount WHEN convert THEN the request has no value`() {
        // Arrange
        val token = Token(symbol = "USDT", contractAddress = TOKEN, decimals = 6)
        val tx = transaction(
            amount = Amount(value = BigDecimal("10"), blockchain = Blockchain.Ethereum, type = AmountType.Token(token)),
        )

        // Act
        val actual = converter.convert(listOf(tx)).data.single()

        // Assert
        assertThat(actual).isEqualTo(Data(from = OWNER, to = MODULE, data = NATIVE_ENTER_DATA))
    }

    private fun transaction(amount: Amount) = SDKTransactionData.Uncompiled(
        amount = amount,
        fee = null,
        sourceAddress = OWNER,
        destinationAddress = MODULE,
        extras = EthereumTransactionExtras(callData = EthereumYieldSupplyEnterWithNativeCallData),
    )

    private companion object {
        const val OWNER = "0x1111111111111111111111111111111111111111"
        const val MODULE = "0x2222222222222222222222222222222222222222"
        const val TOKEN = "0xdAC17F958D2ee523a2206206994597C13D831ec7"
        const val NATIVE_ENTER_DATA = "0x0921ecd4"
    }
}