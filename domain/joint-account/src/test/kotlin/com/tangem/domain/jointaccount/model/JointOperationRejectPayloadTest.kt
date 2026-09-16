package com.tangem.domain.jointaccount.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.math.BigInteger

internal class JointOperationRejectPayloadTest {

    /** The numbers are strings, as the wire body carries them — the backend canonicalizes the request as received */
    @Test
    fun `GIVEN a payload WHEN canonical map THEN the safe tx numbers are decimal strings and the fields are in place`() {
        // Arrange
        val payload = JointOperationRejectPayload(
            operationId = "3f1a2b4c-5d6e-7f80-91a2-b3c4d5e6f708",
            safeTx = SafeTx(
                to = "0x1111111111111111111111111111111111111111",
                value = BigInteger("1000000000000000000"),
                data = "0x",
                operation = SafeTx.OPERATION_CALL,
                safeTxGas = BigInteger("150000"),
                baseGas = BigInteger("110000"),
                gasPrice = BigInteger("60000000000"),
                gasToken = "0x0000000000000000000000000000000000000000",
                refundReceiver = "0xfee0000000000000000000000000000000000001",
                nonce = BigInteger("18446744073709551616"),
            ),
        )

        // Act
        val actual = payload.toCanonicalMap()

        // Assert
        assertThat(actual).isEqualTo(
            mapOf(
                "operationId" to "3f1a2b4c-5d6e-7f80-91a2-b3c4d5e6f708",
                "safeTx" to mapOf(
                    "to" to "0x1111111111111111111111111111111111111111",
                    "value" to "1000000000000000000",
                    "data" to "0x",
                    "operation" to 0,
                    "safeTxGas" to "150000",
                    "baseGas" to "110000",
                    "gasPrice" to "60000000000",
                    "gasToken" to "0x0000000000000000000000000000000000000000",
                    "refundReceiver" to "0xfee0000000000000000000000000000000000001",
                    "nonce" to "18446744073709551616",
                ),
            ),
        )
    }
}