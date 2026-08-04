package com.tangem.data.polymarket.builder

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.domain.polymarket.model.PolymarketApprovalCall
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.extensions.toHexString
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.web3j.crypto.StructuredDataEncoder

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketTypedDataDigestTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN typed data json WHEN hashed THEN matches an independent encoder`(model: DigestModel) {
        // Arrange
        val json = model.json

        // Act
        val actual = EthereumUtils.makeTypedDataHash(json)

        // Assert
        val expected = StructuredDataEncoder(json).hashStructuredData()
        assertThat(actual.toHexString()).isEqualTo(expected.toHexString())
    }

    internal data class DigestModel(val name: String, val json: String) {
        override fun toString(): String = name
    }

    private fun provideTestModels() = listOf(
        DigestModel(
            name = "ClobAuth",
            json = PolymarketTypedDataBuilder.buildClobAuth(
                address = OWNER,
                timestamp = "1735689600",
            ),
        ),
        DigestModel(
            name = "Batch",
            json = PolymarketTypedDataBuilder.buildApprovalsBatch(
                depositWallet = DEPOSIT_WALLET,
                nonce = "0",
                deadline = "1735690200",
                calls = listOf(
                    PolymarketApprovalCall(target = TOKEN, value = "0", data = "0x095ea7b3"),
                    PolymarketApprovalCall(target = EXCHANGE, value = "0", data = "0xa22cb465"),
                ),
            ),
        ),
    )

    private companion object {
        const val OWNER = "0x7E5F4552091A69125d5DfCb7b8C2659029395Bdf"
        const val DEPOSIT_WALLET = "0xfAeA0f08159fcF2f573fE24E9E989B0d48f7651B"
        const val TOKEN = "0xC011a7E12a19f7B1f670d46F03B03f3342E82DFB"
        const val EXCHANGE = "0xE111180000d2663C0091e4f400237545B87B996B"
    }
}