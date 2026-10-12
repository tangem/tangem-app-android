package com.tangem.data.jointaccount.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.store.datasource.jointaccount.models.JointOperationDto
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JointOperationConverterTest {

    private val safeTxConverter = SafeTxConverter()
    private val converter = JointOperationConverter(safeTxConverter = safeTxConverter)

    @Test
    fun `GIVEN an operation dto WHEN convert THEN addresses are lowercased and numbers survive uint256`() {
        // Arrange
        val dto = operationDto(
            chainId = 137,
            safeTx = safeTxDto(
                to = "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48",
                value = UINT256_MAX,
            ),
            initiator = "0x7E5F4552091A69125d5DfCb7b8C2659029395Bdf",
            createdAt = "2026-09-07T09:00:00Z",
            votes = listOf(
                JointOperationDto.VoteDto(
                    address = "0x2B5AD5c4795c026514f8317c7a215E218DcCD6cF",
                    vote = "reject",
                    votedAt = "2026-09-07T10:00:00Z",
                ),
            ),
        )

        // Act
        val actual = converter.convert(dto)

        // Assert
        assertThat(actual).isEqualTo(
            JointOperation(
                operationId = OPERATION_ID,
                chainId = 137,
                safeTx = SafeTx(
                    to = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48",
                    value = BigInteger(UINT256_MAX),
                    data = "0x",
                    operation = 0,
                    safeTxGas = BigInteger("112500"),
                    baseGas = BigInteger("410000"),
                    gasPrice = BigInteger("60000000000"),
                    gasToken = "0x0000000000000000000000000000000000000000",
                    refundReceiver = "0x2222222222222222222222222222222222222222",
                    nonce = BigInteger.valueOf(4),
                ),
                status = JointOperation.Status.AWAITING_SIGNATURES,
                initiator = "0x7e5f4552091a69125d5dfcb7b8c2659029395bdf",
                createdAt = "2026-09-07T09:00:00Z",
                signatures = listOf(
                    JointOperation.MemberSignature(
                        address = "0x2b5ad5c4795c026514f8317c7a215e218dccd6cf",
                        state = JointOperation.MemberSignature.State.DECLINED,
                        at = "2026-09-07T10:00:00Z",
                    ),
                ),
            ),
        )
    }

    @ParameterizedTest
    @ProvideTestModels
    fun convertStatus(model: StatusModel) {
        // Act
        val actual = converter.convert(operationDto(chainId = 1, status = model.raw))

        // Assert
        assertThat(actual.status).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN every vote the backend can send WHEN convert THEN approve signs, decline declines, the rest is unknown`() {
        // Arrange
        val dto = operationDto(
            chainId = 1,
            votes = listOf(
                JointOperationDto.VoteDto(address = "0xa", vote = "approve", votedAt = "2026-09-07T10:00:00Z"),
                JointOperationDto.VoteDto(address = "0xb", vote = "reject", votedAt = "2026-09-07T10:01:00Z"),
                JointOperationDto.VoteDto(address = "0xc", vote = "abstain", votedAt = null),
            ),
        )

        // Act
        val actual = converter.convert(dto).signatures.map { it.state }

        // Assert
        assertThat(actual).containsExactly(
            JointOperation.MemberSignature.State.SIGNED,
            JointOperation.MemberSignature.State.DECLINED,
            JointOperation.MemberSignature.State.UNKNOWN,
        ).inOrder()
    }

    internal data class StatusModel(val raw: String, val expected: JointOperation.Status)

    private fun provideTestModels() = listOf(
        StatusModel(raw = "awaiting_signatures", expected = JointOperation.Status.AWAITING_SIGNATURES),
        StatusModel(raw = "pending", expected = JointOperation.Status.PENDING),
        StatusModel(raw = "executed", expected = JointOperation.Status.UNKNOWN),
        StatusModel(raw = "brand_new_status", expected = JointOperation.Status.UNKNOWN),
    )

    @Test
    fun `GIVEN two in-flight operations on one chain WHEN convert THEN the first by order wins`() {
        // Arrange
        val first = operationDto(chainId = 1, operationId = "first")
        val duplicate = operationDto(chainId = 1, operationId = "duplicate")
        val other = operationDto(chainId = 137, operationId = "other")

        // Act
        val actual = converter.convertInFlight(listOf(first, duplicate, other))

        // Assert
        assertThat(actual.mapValues { it.value.operationId }).containsExactly(1L, "first", 137L, "other")
    }

    /** One bad operation must not cost the wallet every joint account it has — the fetch would fall to cache */
    @Test
    fun `GIVEN one in-flight operation out of contract WHEN convert THEN only that one is dropped`() {
        // Arrange
        val broken = operationDto(chainId = 1, operationId = "broken", safeTx = safeTxDto(nonce = UINT256_MAX))
        val sound = operationDto(chainId = 137, operationId = "sound")

        // Act
        val actual = converter.convertInFlight(listOf(broken, sound))

        // Assert
        assertThat(actual.mapValues { it.value.operationId }).containsExactly(137L, "sound")
    }

    @Test
    fun `GIVEN a safe tx round trip WHEN convert back THEN the dto matches the source`() {
        // Arrange
        val dto = safeTxDto(to = "0x1111111111111111111111111111111111111111", value = UINT256_MAX)

        // Act
        val actual = safeTxConverter.convertBack(safeTxConverter.convert(dto))

        // Assert
        assertThat(actual).isEqualTo(dto)
    }

    /**
     * The contract validates the field as `^(0|[1-9]\d{0,17})$`, and the persisted copy keeps a `Long`. A wider
     * value is refused while reading, so the fetch drops that one operation instead of failing at the store.
     */
    @Test
    fun `GIVEN a nonce wider than the contract allows WHEN convert THEN it is refused while reading`() {
        // Act
        val actual = runCatching { safeTxConverter.convert(safeTxDto(nonce = UINT256_MAX)) }.exceptionOrNull()

        // Assert
        assertThat(actual).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `GIVEN the largest nonce the contract allows WHEN convert back THEN it survives as a decimal string`() {
        // Arrange
        val safeTx = safeTxConverter.convert(safeTxDto(nonce = MAX_CONTRACT_NONCE))

        // Act
        val actual = safeTxConverter.convertBack(safeTx)

        // Assert
        assertThat(actual.nonce).isEqualTo(MAX_CONTRACT_NONCE)
    }

    private fun operationDto(
        chainId: Long,
        operationId: String = OPERATION_ID,
        safeTx: JointOperationDto.SafeTxDto = safeTxDto(),
        initiator: String? = null,
        createdAt: String? = null,
        votes: List<JointOperationDto.VoteDto> = emptyList(),
        status: String = "awaiting_signatures",
    ) = JointOperationDto(
        operationId = operationId,
        chainId = chainId,
        safeTx = safeTx,
        status = status,
        initiator = initiator,
        createdAt = createdAt,
        votes = votes,
    )

    private fun safeTxDto(
        to: String = "0x1111111111111111111111111111111111111111",
        value: String = "1000000000000000000",
        nonce: String = "4",
    ) = JointOperationDto.SafeTxDto(
        to = to,
        value = value,
        data = "0x",
        operation = 0,
        safeTxGas = "112500",
        baseGas = "410000",
        gasPrice = "60000000000",
        gasToken = "0x0000000000000000000000000000000000000000",
        refundReceiver = "0x2222222222222222222222222222222222222222",
        nonce = nonce,
    )

    private companion object {
        const val OPERATION_ID = "3f1a2b4c-5d6e-7f80-91a2-b3c4d5e6f708"
        const val MAX_CONTRACT_NONCE = "999999999999999999"
        const val UINT256_MAX =
            "115792089237316195423570985008687907853269984665640564039457584007913129639935"
    }
}