package com.tangem.data.pay.util

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.extensions.toHexString
import com.tangem.data.pay.util.TangemPayWithdrawDataVerifier.Expectation
import com.tangem.data.pay.util.TangemPayWithdrawDataVerifier.Mismatch
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.web3j.crypto.StructuredDataEncoder
import java.math.BigDecimal

/**
 * The digest is produced by the blockchain SDK, so the hashes here come from an independent EIP-712 implementation
 * (web3j) — otherwise the check would either wave through every payload or refuse every one of them.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayWithdrawDataVerifierTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN withdraw data WHEN verify THEN the matching verdict returned`(model: VerifyModel) {
        // Act
        val actual = TangemPayWithdrawDataVerifier.verify(
            hash = model.hash ?: digestOf(model.structuredData),
            structuredData = model.structuredData,
            expectation = model.expectation,
        )

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Suppress("LongMethod")
    private fun provideTestModels() = listOf(
        VerifyModel(
            structuredData = typedData(),
            expected = Unit.right(),
        ),
        VerifyModel(
            structuredData = typedData(),
            hash = digestOf(typedData()).removePrefix("0x"),
            expected = Unit.right(),
        ),
        VerifyModel(
            structuredData = typedData(),
            hash = digestOf(typedData()).replace(oldValue = "0x", newValue = "0X"),
            expected = Unit.right(),
        ),
        VerifyModel(
            structuredData = typedData(chainId = "\"8453\"", amount = "\"8460000\""),
            expected = Unit.right(),
        ),
        VerifyModel(
            structuredData = typedData(asset = ASSET.uppercase().replace("0X", "0x")),
            expectation = expectation(recipientAddress = RECIPIENT.uppercase().replace("0X", "0x")),
            expected = Unit.right(),
        ),
        VerifyModel(
            structuredData = typedData(chainId = "56", amount = "8460000000000000000"),
            expectation = expectation(chainId = 56, tokenDecimals = 18),
            expected = Unit.right(),
        ),
        VerifyModel(
            structuredData = typedData(chainId = "84532"),
            expectation = expectation(chainId = null, tokenContractAddress = null),
            expected = Unit.right(),
        ),
        VerifyModel(
            structuredData = typedData(amount = "1000000000"),
            expectation = expectation(amountInCents = BigDecimal("100000"), recipientAddress = null),
            expected = Unit.right(),
        ),
        VerifyModel(
            structuredData = typedData(chainId = "137"),
            expected = Mismatch.NETWORK.left(),
        ),
        VerifyModel(
            structuredData = typedData(asset = FOREIGN_ADDRESS),
            expected = Mismatch.NETWORK.left(),
        ),
        VerifyModel(
            structuredData = typedData(recipient = FOREIGN_ADDRESS),
            expected = Mismatch.RECIPIENT.left(),
        ),
        VerifyModel(
            structuredData = typedData(amount = "8470000"),
            expected = Mismatch.AMOUNT.left(),
        ),
        VerifyModel(
            structuredData = typedData(amount = "846000000"),
            expected = Mismatch.AMOUNT.left(),
        ),
        VerifyModel(
            structuredData = typedData(),
            hash = digestOf(typedData(amount = "8470000")),
            expected = Mismatch.HASH.left(),
        ),
        VerifyModel(
            structuredData = UNDECLARED_RECIPIENT,
            hash = "0xdeadbeef",
            expected = Mismatch.RECIPIENT.left(),
        ),
        VerifyModel(
            structuredData = typedData().replace(
                oldValue = "\"primaryType\": \"Withdraw\"",
                newValue = "\"primaryType\": \"Deposit\"",
            ),
            hash = "0xdeadbeef",
            expected = Mismatch.NETWORK.left(),
        ),
        VerifyModel(
            structuredData = typedData().replace(oldValue = "\"chainId\", \"type\"", newValue = "\"salt\", \"type\""),
            hash = "0xdeadbeef",
            expected = Mismatch.NETWORK.left(),
        ),
        VerifyModel(
            structuredData = null,
            hash = digestOf(typedData()),
            expected = Mismatch.NO_STRUCTURED_DATA.left(),
        ),
        VerifyModel(
            structuredData = "{not json",
            hash = "0xdeadbeef",
            expected = Mismatch.MALFORMED_STRUCTURED_DATA.left(),
        ),
    )

    data class VerifyModel(
        val structuredData: String?,
        val hash: String? = null,
        val expectation: Expectation = expectation(),
        val expected: Either<Mismatch, Unit>,
    )

    private companion object {
        const val ASSET = "0x833589fcD6eDb6E08f4c7C32D4f71b54bdA02913"
        const val RECIPIENT = "0x059dbd5f24a000d28f83268db1c85c9c3ab6a123"
        const val FOREIGN_ADDRESS = "0xfde4C96c8593536E31F229EA8f37b2ADa2699bb2"

        val UNDECLARED_RECIPIENT = """
            {
              "types": {
                "EIP712Domain": [
                  {"name": "name", "type": "string"},
                  {"name": "chainId", "type": "uint256"}
                ],
                "Withdraw": [
                  {"name": "asset", "type": "address"},
                  {"name": "to", "type": "address"},
                  {"name": "amount", "type": "uint256"}
                ]
              },
              "primaryType": "Withdraw",
              "domain": {"name": "Collateral", "chainId": 8453},
              "message": {"asset": "$ASSET", "recipient": "$RECIPIENT", "to": "$FOREIGN_ADDRESS", "amount": 8460000}
            }
        """.trimIndent()

        fun expectation(
            amountInCents: BigDecimal = BigDecimal("846"),
            tokenDecimals: Int = 6,
            recipientAddress: String? = RECIPIENT,
            chainId: Long? = 8453,
            tokenContractAddress: String? = ASSET.lowercase(),
        ) = Expectation(
            amountInCents = amountInCents,
            tokenDecimals = tokenDecimals,
            recipientAddress = recipientAddress,
            chainId = chainId,
            tokenContractAddress = tokenContractAddress,
        )

        fun digestOf(structuredData: String?): String =
            "0x" + StructuredDataEncoder(structuredData).hashStructuredData().toHexString()

        fun typedData(
            chainId: String = "8453",
            asset: String = ASSET,
            recipient: String = RECIPIENT,
            amount: String = "8460000",
        ) = """
            {
              "types": {
                "EIP712Domain": [
                  {"name": "name", "type": "string"},
                  {"name": "chainId", "type": "uint256"}
                ],
                "Withdraw": [
                  {"name": "asset", "type": "address"},
                  {"name": "recipient", "type": "address"},
                  {"name": "amount", "type": "uint256"}
                ]
              },
              "primaryType": "Withdraw",
              "domain": {"name": "Collateral", "chainId": $chainId},
              "message": {"asset": "$asset", "recipient": "$recipient", "amount": $amount}
            }
        """.trimIndent()
    }
}