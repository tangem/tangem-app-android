package com.tangem.data.walletconnect.network.ethereum

import com.google.common.truth.Truth.assertThat
import com.tangem.data.walletconnect.network.ethereum.WcEthTypedDataPolicy.Violation
import com.tangem.domain.transaction.models.GaslessBatchTransactionData
import com.tangem.domain.transaction.models.GaslessTransactionData
import com.tangem.domain.transaction.usecase.gasless.Eip712TypedDataBuilder
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class WcEthTypedDataPolicyTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN typed data WHEN findViolation THEN returns expected violation`(model: PolicyModel) {
        // Act
        val actual = WcEthTypedDataPolicy.findViolation(typedData = model.typedData, signerAddress = model.signer)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @ParameterizedTest
    @MethodSource("provideGaslessPayloads")
    fun `GIVEN real gasless payload for signer WHEN findViolation THEN rejected as gasless primary type`(
        model: GaslessPayloadModel,
    ) {
        // Act
        val actual = WcEthTypedDataPolicy.findViolation(typedData = model.typedData, signerAddress = SIGNER)

        // Assert
        assertThat(actual).isEqualTo(Violation.GASLESS_PRIMARY_TYPE)
    }

    @ParameterizedTest
    @MethodSource("provideHostileVerifyingContracts")
    fun `GIVEN hostile verifyingContract literal WHEN findViolation THEN does not throw`(verifyingContract: String) {
        // Arrange
        val typedData = typedData(domain = """{"name":"Mail","verifyingContract":"$verifyingContract"}""")

        // Act
        val failure = runCatching { WcEthTypedDataPolicy.findViolation(typedData, signerAddress = SIGNER) }
            .exceptionOrNull()

        // Assert
        assertThat(failure).isNull()
    }

    data class PolicyModel(
        val description: String,
        val typedData: String,
        val signer: String,
        val expected: Violation?,
    ) {
        override fun toString(): String = description
    }

    data class GaslessPayloadModel(val description: String, val typedData: String) {
        override fun toString(): String = description
    }

    private fun provideTestModels() = listOf(
        PolicyModel(
            description = "verifyingContract equals signer in different case",
            typedData = typedData(domain = """{"name":"Mail","verifyingContract":"$SIGNER_UPPER"}"""),
            signer = SIGNER,
            expected = Violation.SIGNER_IS_VERIFYING_CONTRACT,
        ),
        PolicyModel(
            description = "verifyingContract is zero-padded 32-byte signer",
            typedData = typedData(domain = """{"name":"Mail","verifyingContract":"$SIGNER_PADDED"}"""),
            signer = SIGNER,
            expected = Violation.SIGNER_IS_VERIFYING_CONTRACT,
        ),
        PolicyModel(
            description = "verifyingContract is decimal signer",
            typedData = typedData(domain = """{"name":"Mail","verifyingContract":"$SIGNER_DECIMAL"}"""),
            signer = SIGNER,
            expected = Violation.SIGNER_IS_VERIFYING_CONTRACT,
        ),
        PolicyModel(
            description = "executor domain name with foreign verifyingContract",
            typedData = typedData(
                domain = """{"name":"tangem7702gaslessexecutor","verifyingContract":"$FOREIGN_CONTRACT"}""",
            ),
            signer = SIGNER,
            expected = Violation.GASLESS_EXECUTOR_DOMAIN,
        ),
        PolicyModel(
            description = "primaryType GaslessTransaction",
            typedData = typedData(
                domain = """{"name":"Mail","verifyingContract":"$FOREIGN_CONTRACT"}""",
                primaryType = " GaslessTransaction ",
            ),
            signer = SIGNER,
            expected = Violation.GASLESS_PRIMARY_TYPE,
        ),
        PolicyModel(
            description = "primaryType GaslessBatchTransaction",
            typedData = typedData(
                domain = """{"name":"Mail","verifyingContract":"$FOREIGN_CONTRACT"}""",
                primaryType = "gaslessbatchtransaction",
            ),
            signer = SIGNER,
            expected = Violation.GASLESS_PRIMARY_TYPE,
        ),
        PolicyModel(
            description = "invalid JSON",
            typedData = "not json",
            signer = SIGNER,
            expected = Violation.MALFORMED_PAYLOAD,
        ),
        PolicyModel(
            description = "domain is a string",
            typedData = typedData(domain = "\"$SIGNER\""),
            signer = SIGNER,
            expected = Violation.MALFORMED_PAYLOAD,
        ),
        PolicyModel(
            description = "verifyingContract is a JSON number",
            typedData = typedData(domain = """{"name":"Mail","verifyingContract":1}"""),
            signer = SIGNER,
            expected = Violation.MALFORMED_PAYLOAD,
        ),
        PolicyModel(
            description = "verifyingContract is decimal with huge positive exponent",
            typedData = typedData(domain = """{"name":"Mail","verifyingContract":"1e100000000"}"""),
            signer = SIGNER,
            expected = Violation.MALFORMED_PAYLOAD,
        ),
        PolicyModel(
            description = "verifyingContract is decimal with huge negative exponent",
            typedData = typedData(domain = """{"name":"Mail","verifyingContract":"1e-100000000"}"""),
            signer = SIGNER,
            expected = Violation.MALFORMED_PAYLOAD,
        ),
        PolicyModel(
            description = "verifyingContract is decimal wider than uint256",
            typedData = typedData(domain = """{"name":"Mail","verifyingContract":"1e78"}"""),
            signer = SIGNER,
            expected = Violation.MALFORMED_PAYLOAD,
        ),
        PolicyModel(
            description = "verifyingContract normalization overflows scale",
            typedData = typedData(domain = """{"name":"Mail","verifyingContract":"100e2147483647"}"""),
            signer = SIGNER,
            expected = Violation.MALFORMED_PAYLOAD,
        ),
        PolicyModel(
            description = "verifyingContract exponent at Int.MAX_VALUE",
            typedData = typedData(domain = """{"name":"Mail","verifyingContract":"1e2147483647"}"""),
            signer = SIGNER,
            expected = Violation.MALFORMED_PAYLOAD,
        ),
        PolicyModel(
            description = "verifyingContract literal is too long",
            typedData = typedData(domain = """{"name":"Mail","verifyingContract":"0x${"0".repeat(80)}"}"""),
            signer = SIGNER,
            expected = Violation.MALFORMED_PAYLOAD,
        ),
        PolicyModel(
            description = "verifyingContract is decimal signer in exponent notation",
            typedData = typedData(domain = """{"name":"Mail","verifyingContract":"${SIGNER_DECIMAL}0e-1"}"""),
            signer = SIGNER,
            expected = Violation.SIGNER_IS_VERIFYING_CONTRACT,
        ),
        PolicyModel(
            description = "EIP-2612 Permit for a token",
            typedData = typedData(
                domain = """{"name":"USD Coin","version":"2","chainId":1,"verifyingContract":"$USDC"}""",
                primaryType = "Permit",
            ),
            signer = SIGNER,
            expected = null,
        ),
        PolicyModel(
            description = "Permit2 PermitSingle",
            typedData = typedData(
                domain = """{"name":"Permit2","chainId":1,"verifyingContract":"$PERMIT2"}""",
                primaryType = "PermitSingle",
            ),
            signer = SIGNER,
            expected = null,
        ),
        PolicyModel(
            description = "payload without domain",
            typedData = """{"primaryType":"Mail","types":{},"message":{}}""",
            signer = SIGNER,
            expected = null,
        ),
    )

    @Suppress("UnusedPrivateMember")
    private fun provideGaslessPayloads() = listOf(
        GaslessPayloadModel(
            description = "build",
            typedData = Eip712TypedDataBuilder.build(
                gaslessTransaction = GaslessTransactionData(
                    transaction = gaslessTransaction(),
                    fee = gaslessFee(),
                    nonce = BigInteger.ZERO,
                ),
                chainId = 1,
                verifyingContract = SIGNER,
            ),
        ),
        GaslessPayloadModel(
            description = "buildBatch",
            typedData = Eip712TypedDataBuilder.buildBatch(
                gaslessBatch = GaslessBatchTransactionData(
                    transactions = listOf(gaslessTransaction(), gaslessTransaction()),
                    fee = gaslessFee(),
                    nonce = BigInteger.ZERO,
                ),
                chainId = 1,
                verifyingContract = SIGNER,
            ),
        ),
    )

    @Suppress("UnusedPrivateMember")
    private fun provideHostileVerifyingContracts() = listOf(
        "100e2147483647",
        "1e2147483647",
        "1e2147483648",
        "1e-2147483647",
        "1e-2147483648",
        "1e-2147483649",
        "0.1e-2147483647",
        "${"9".repeat(79)}e-2147483647",
        "1E+2147483647",
        "-0",
        "-1",
        "+0",
        "0x",
        "0X",
        "0x-1",
        "0x+1",
        "+$SIGNER_DECIMAL",
        "  $SIGNER  ",
        "\\u0661\\u0662",
        "",
        "e",
        ".",
        "NaN",
        "Infinity",
    )

    private fun typedData(domain: String, primaryType: String = "Mail"): String =
        """{"types":{},"primaryType":"$primaryType","domain":$domain,"message":{}}"""

    private fun gaslessTransaction() = GaslessTransactionData.Transaction(
        to = FOREIGN_CONTRACT,
        value = BigInteger.ZERO,
        gasLimit = BigInteger.valueOf(120_000),
        data = byteArrayOf(0x12, 0x34),
    )

    private fun gaslessFee() = GaslessTransactionData.Fee(
        feeToken = USDC,
        maxTokenFee = BigInteger.TEN,
        coinPriceInToken = BigInteger.ONE,
        feeTransferGasLimit = BigInteger.valueOf(100),
        baseGas = BigInteger.valueOf(60_000),
        feeReceiver = FOREIGN_CONTRACT,
    )

    private companion object {
        const val SIGNER = "0x1234567890abcdef1234567890abcdef12345678"
        const val SIGNER_UPPER = "0x1234567890ABCDEF1234567890ABCDEF12345678"
        const val SIGNER_PADDED = "0x0000000000000000000000001234567890abcdef1234567890abcdef12345678"
        const val FOREIGN_CONTRACT = "0x9999999999999999999999999999999999999999"
        const val USDC = "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48"
        const val PERMIT2 = "0x000000000022D473030F116dDEE9F6B43aC78BA3"
        val SIGNER_DECIMAL: String = BigInteger(SIGNER.removePrefix("0x"), 16).toString()
    }
}