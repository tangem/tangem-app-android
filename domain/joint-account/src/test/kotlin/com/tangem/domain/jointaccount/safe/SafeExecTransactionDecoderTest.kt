package com.tangem.domain.jointaccount.safe

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.jointaccount.model.SafeExecution
import com.tangem.domain.jointaccount.model.SafeInnerCall
import com.tangem.domain.jointaccount.model.SafeRefund
import com.tangem.domain.jointaccount.safe.SafeExecutionFixtures.AGGREGATE3_CALLDATA
import com.tangem.domain.jointaccount.safe.SafeExecutionFixtures.EXEC_TRANSACTION_CALLDATA
import com.tangem.domain.jointaccount.safe.SafeExecutionFixtures.RECIPIENT
import com.tangem.domain.jointaccount.safe.SafeExecutionFixtures.RELAYER
import com.tangem.domain.jointaccount.safe.SafeExecutionFixtures.SAFE
import com.tangem.domain.jointaccount.safe.SafeExecutionFixtures.ZERO_ADDRESS
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SafeExecTransactionDecoderTest {

    @Test
    fun `GIVEN direct execTransaction calldata WHEN decode THEN every field is read`() {
        // Act
        val actual = SafeExecTransactionDecoder.decode(EXEC_TRANSACTION_CALLDATA)

        // Assert
        assertThat(actual).isEqualTo(
            SafeExecution(
                call = SafeInnerCall(to = RECIPIENT, value = BigInteger("10935223680498695559"), data = "0x"),
                operation = 0,
                refund = SafeRefund(
                    safeTxGas = BigInteger.valueOf(31_500),
                    baseGas = BigInteger.valueOf(110_000),
                    gasPrice = BigInteger.valueOf(446_585_022_236),
                    gasToken = ZERO_ADDRESS,
                    refundReceiver = RELAYER,
                ),
                signaturesCount = 2,
            ),
        )
        assertThat(actual?.refund?.maxPayment).isEqualTo(BigInteger("63191780646394000"))
    }

    @Test
    fun `GIVEN aggregate3 bundle WHEN decode THEN the bundled execTransaction is read`() {
        // Act
        val actual = SafeExecTransactionDecoder.decode(AGGREGATE3_CALLDATA)

        // Assert
        assertThat(actual).isEqualTo(
            SafeExecution(
                call = SafeInnerCall(to = RECIPIENT, value = BigInteger("1093522368049869555"), data = "0x"),
                operation = 0,
                refund = SafeRefund(
                    safeTxGas = BigInteger.valueOf(31_500),
                    baseGas = BigInteger.valueOf(410_000),
                    gasPrice = BigInteger.valueOf(546_234_576_286),
                    gasToken = ZERO_ADDRESS,
                    refundReceiver = RELAYER,
                ),
                signaturesCount = 2,
                safe = SAFE,
            ),
        )
    }

    @Test
    fun `GIVEN uppercase calldata without prefix WHEN decode THEN it is read the same`() {
        // Act
        val actual = SafeExecTransactionDecoder.decode(EXEC_TRANSACTION_CALLDATA.removePrefix("0x").uppercase())

        // Assert
        assertThat(actual).isEqualTo(SafeExecTransactionDecoder.decode(EXEC_TRANSACTION_CALLDATA))
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN calldata that is not a Safe execution WHEN decode THEN null`(callData: String) {
        // Act
        val actual = SafeExecTransactionDecoder.decode(callData)

        // Assert
        assertThat(actual).isNull()
    }

    private fun provideTestModels() = listOf(
        // ERC-20 transfer(address,uint256): a different selector
        "0xa9059cbb0000000000000000000000006dbd0c1957dfe9d85f74b8fe3a355b268a39ec91" +
            "0000000000000000000000000000000000000000000000000de0b6b3a7640000",
        // the right selector, but the payload ends inside the head words
        EXEC_TRANSACTION_CALLDATA.take(n = 200),
        // the right selector, but the signatures offset points past the payload
        EXEC_TRANSACTION_CALLDATA.take(n = 10 + 64 * 10),
        // not hex at all
        "0x6a761202zz",
        // odd number of hex characters
        "0x6a7612020",
        "0x",
        "",
    )
}