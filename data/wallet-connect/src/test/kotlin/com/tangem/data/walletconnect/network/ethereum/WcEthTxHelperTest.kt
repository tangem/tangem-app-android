package com.tangem.data.walletconnect.network.ethereum

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.walletconnect.model.WcEthTransactionParams
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

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

    private fun txParams() = WcEthTransactionParams(
        from = "0xSender",
        to = "0xRecipient",
        data = null,
        gas = null,
        gasPrice = null,
        value = "0x0de0b6b3a7640000",
        nonce = null,
    )
}