package com.tangem.data.walletconnect.network.ethereum

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.smartcontract.CompiledSmartContractCallData
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.walletconnect.model.WcEthTransactionParams
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class WcEthTxHelperTest {

    private val helper = WcEthTxHelper(
        singleAccountListSupplier = mockk(),
        ethSpecificFee = mockk(),
    )

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN evm network WHEN createTransactionData THEN wei value is converted with 18 decimals`(
        blockchain: Blockchain,
    ) {
        // Arrange
        val network = MockCryptoCurrencyFactory().createCoin(blockchain).network

        // Act
        val actual = helper.createTransactionData(dAppFee = null, network = network, txParams = txParams())

        // Assert
        assertThat(actual!!.amount.value).isEquivalentAccordingToCompareTo(BigDecimal.ONE)
        assertThat(actual.amount.decimals).isEqualTo(18)
    }

    private fun provideTestModels() = listOf(Blockchain.Ethereum, Blockchain.Arc)

    @Test
    fun `GIVEN well-formed hex fields WHEN createTransactionData THEN they are decoded as sent`() {
        val actual = helper.createTransactionData(
            dAppFee = null,
            network = ethereum,
            txParams = txParams(value = "0x0", data = "0xa9059cbb", nonce = "0x1A"),
        )

        assertThat(actual!!.amount.value).isEquivalentAccordingToCompareTo(BigDecimal.ZERO)
        val extras = actual.extras as EthereumTransactionExtras
        assertThat((extras.callData as CompiledSmartContractCallData).data)
            .isEqualTo(byteArrayOf(0xa9.toByte(), 0x05, 0x9c.toByte(), 0xbb.toByte()))
        assertThat(extras.nonce).isEqualTo(BigInteger.valueOf(26))
    }

    @Test
    fun `GIVEN bare 0x value and data WHEN createTransactionData THEN zero value and no call data`() {
        val actual = helper.createTransactionData(
            dAppFee = null,
            network = ethereum,
            txParams = txParams(value = "0x", data = "0x"),
        )

        assertThat(actual!!.amount.value).isEquivalentAccordingToCompareTo(BigDecimal.ZERO)
        assertThat((actual.extras as EthereumTransactionExtras).callData).isNull()
    }

    @Test
    fun `GIVEN decimal value without 0x WHEN createTransactionData THEN rejected instead of read as hex`() {
        // "1000000000000000000" read as hex is ~4.7e21 wei, not 1 ETH
        val actual = helper.createTransactionData(
            dAppFee = null,
            network = ethereum,
            txParams = txParams(value = "1000000000000000000"),
        )

        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN non-hex value WHEN createTransactionData THEN rejected instead of defaulting to zero`() {
        val actual = helper.createTransactionData(
            dAppFee = null,
            network = ethereum,
            txParams = txParams(value = "0xzz"),
        )

        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN odd-length call data WHEN createTransactionData THEN rejected instead of dropping a nibble`() {
        val actual = helper.createTransactionData(
            dAppFee = null,
            network = ethereum,
            txParams = txParams(data = "0xa9059cb"),
        )

        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN non-hex nonce WHEN createTransactionData THEN rejected instead of defaulting to zero`() {
        val actual = helper.createTransactionData(
            dAppFee = null,
            network = ethereum,
            txParams = txParams(nonce = "12"),
        )

        assertThat(actual).isNull()
    }

    private val ethereum by lazy { MockCryptoCurrencyFactory().createCoin(Blockchain.Ethereum).network }

    private fun txParams(
        value: String? = "0x0de0b6b3a7640000",
        data: String? = null,
        nonce: String? = null,
    ) = WcEthTransactionParams(
        from = "0xSender",
        to = "0xRecipient",
        data = data,
        gas = null,
        gasPrice = null,
        value = value,
        nonce = nonce,
    )
}