package com.tangem.features.send.send.joint

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

internal class JointSendFeeConverterTest {

    private val converter = JointSendFeeConverter(network = fixtureCoin.network)

    @Test
    fun `GIVEN a safe tx WHEN converted THEN the fee is the gas price over the whole gas in the network coin`() {
        // Arrange
        val safeTx = createSafeTx(
            safeTxGas = BigInteger.valueOf(150_000),
            baseGas = BigInteger.valueOf(110_000),
            gasPrice = BigInteger.valueOf(20_000_000_000),
        )

        // Act
        val actual = converter.convert(safeTx)

        // Assert
        assertThat(actual).isEqualTo(
            TransactionFee.Single(
                normal = Fee.Ethereum.Legacy(
                    amount = Amount(value = BigDecimal("0.005200000000000000"), blockchain = Blockchain.Ethereum),
                    gasLimit = BigInteger.valueOf(260_000),
                    gasPrice = BigInteger.valueOf(20_000_000_000),
                ),
            ),
        )
    }
}