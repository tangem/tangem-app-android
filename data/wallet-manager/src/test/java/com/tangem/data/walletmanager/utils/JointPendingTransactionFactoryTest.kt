package com.tangem.data.walletmanager.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionStatus
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.model.JointOperationTransfer
import com.tangem.domain.jointaccount.model.JointTransferIntent
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.jointaccount.safe.JointOperationTransferResolver
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigInteger
import java.time.Instant

/**
 * The pending row stands in for an operation that has no transaction on chain, so it is keyed by the operation id
 * and dated by the initiator's signature.
 */
internal class JointPendingTransactionFactoryTest {

    private val supplier: SingleJointAccountListSupplier = mockk()
    private val ethereum = MockCryptoCurrencyFactory().ethereum

    /** Reading a transfer out of a payload is the resolver's own subject, covered by its tests in the data layer */
    private val transferResolver: JointOperationTransferResolver = mockk {
        every { resolve(any(), any(), any(), any()) } returns JointOperationTransfer(
            currency = ethereum,
            intent = JointTransferIntent.Native(recipient = RECIPIENT, amountWei = AMOUNT),
        )
    }

    private val factory = JointPendingTransactionFactory(
        jointAccountListSupplier = supplier,
        transferResolver = transferResolver,
    )

    private val userWalletId = UserWalletId("011")

    @Test
    fun `GIVEN no account holds the Safe address WHEN create THEN no pending transaction`() = runTest {
        // Arrange
        every { supplier(userWalletId) } returns flowOf(listOf(jointAccount(safeAddress = "0xanothersafe")))

        // Act
        val actual = create()

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN the account has no operation in this chain WHEN create THEN no pending transaction`() = runTest {
        // Arrange
        every { supplier(userWalletId) } returns flowOf(listOf(jointAccount(operations = emptyMap())))

        // Act
        val actual = create()

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN a native transfer operation WHEN create THEN an unconfirmed transaction keyed by the operation id`() =
        runTest {
            // Arrange
            every { supplier(userWalletId) } returns flowOf(listOf(jointAccount()))

            // Act
            val actual = create()

            // Assert
            assertThat(actual?.hash).isEqualTo(OPERATION_ID)
            assertThat(actual?.status).isEqualTo(TransactionStatus.Unconfirmed)
            assertThat(actual?.sourceAddress).isEqualTo(SAFE_ADDRESS)
            assertThat(actual?.destinationAddress).isEqualTo(RECIPIENT)
            assertThat(actual?.contractAddress).isNull()
            assertThat(actual?.amount?.value).isEqualTo(AMOUNT.toBigDecimal().movePointLeft(ethereum.decimals))
        }

    @ParameterizedTest
    @EnumSource(value = JointOperation.Status::class, names = ["PENDING", "UNKNOWN"])
    fun `GIVEN the operation left the signature collection WHEN create THEN no pending transaction`(
        status: JointOperation.Status,
    ) = runTest {
        // Arrange
        every { supplier(userWalletId) } returns flowOf(
            listOf(jointAccount(operations = mapOf(ETHEREUM_CHAIN_ID to operation(status = status)))),
        )

        // Act
        val actual = create()

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN a pending operation WHEN create THEN the fee is the refund the Safe owes the executor`() = runTest {
        // Arrange
        every { supplier(userWalletId) } returns flowOf(listOf(jointAccount()))

        // Act
        val actual = create()

        // Assert
        val expected = GAS_PRICE.multiply(SAFE_TX_GAS + BASE_GAS).toBigDecimal().movePointLeft(ethereum.decimals)
        assertThat(actual?.fee?.amount?.value).isEqualTo(expected)
        assertThat(actual?.fee?.amount?.currencySymbol).isEqualTo(Blockchain.Ethereum.currency)
    }

    @Test
    fun `GIVEN a co-signer answered before the initiator WHEN create THEN the row is dated by the initiator`() =
        runTest {
            // Arrange
            val operation = operation(
                signatures = listOf(
                    JointOperation.MemberSignature(
                        address = MEMBER,
                        state = JointOperation.MemberSignature.State.SIGNED,
                        at = MEMBER_SIGNED_AT,
                    ),
                    JointOperation.MemberSignature(
                        address = INITIATOR,
                        state = JointOperation.MemberSignature.State.SIGNED,
                        at = CREATED_AT,
                    ),
                ),
            )
            every { supplier(userWalletId) } returns flowOf(
                listOf(jointAccount(operations = mapOf(ETHEREUM_CHAIN_ID to operation))),
            )

            // Act
            val actual = create()

            // Assert
            assertThat(actual?.date?.timeInMillis).isEqualTo(Instant.parse(CREATED_AT).toEpochMilli())
        }

    private suspend fun create() = factory.create(
        userWalletId = userWalletId,
        currency = ethereum,
        safeAddress = SAFE_ADDRESS,
        blockchain = Blockchain.Ethereum,
    )

    private fun jointAccount(
        safeAddress: String? = SAFE_ADDRESS,
        operations: Map<Long, JointOperation> = mapOf(ETHEREUM_CHAIN_ID to operation()),
    ) = JointAccount(
        cryptoAccountId = "account",
        membersCount = 3,
        threshold = 2,
        safeAddress = safeAddress,
        status = JointAccount.Status.ACTIVE,
        members = emptyList(),
        source = StatusSource.ACTUAL,
        inFlightOperations = operations,
    )

    private fun operation(
        status: JointOperation.Status = JointOperation.Status.AWAITING_SIGNATURES,
        signatures: List<JointOperation.MemberSignature> = listOf(
            JointOperation.MemberSignature(
                address = INITIATOR,
                state = JointOperation.MemberSignature.State.SIGNED,
                at = CREATED_AT,
            ),
        ),
    ) = JointOperation(
        operationId = OPERATION_ID,
        chainId = ETHEREUM_CHAIN_ID,
        safeTx = SafeTx(
            to = RECIPIENT,
            value = AMOUNT,
            data = "0x",
            operation = 0,
            safeTxGas = SAFE_TX_GAS,
            baseGas = BASE_GAS,
            gasPrice = GAS_PRICE,
            gasToken = ZERO_ADDRESS,
            refundReceiver = ZERO_ADDRESS,
            nonce = BigInteger.ZERO,
        ),
        status = status,
        initiator = INITIATOR,
        signatures = signatures,
    )

    private companion object {
        const val ETHEREUM_CHAIN_ID = 1L
        const val OPERATION_ID = "27ca1f4e-0f2e-4a1e-9d0b-3b9f6a1c2d3e"
        const val SAFE_ADDRESS = "0x1111111111111111111111111111111111111111"
        const val RECIPIENT = "0x2222222222222222222222222222222222222222"
        const val INITIATOR = "0x3333333333333333333333333333333333333333"
        const val MEMBER = "0x4444444444444444444444444444444444444444"
        const val ZERO_ADDRESS = "0x0000000000000000000000000000000000000000"
        const val CREATED_AT = "2026-09-01T12:00:00Z"
        const val MEMBER_SIGNED_AT = "2026-09-01T10:00:00Z"
        val AMOUNT: BigInteger = BigInteger("500000000000000000")
        val SAFE_TX_GAS: BigInteger = BigInteger.valueOf(21_000)
        val BASE_GAS: BigInteger = BigInteger.valueOf(110_000)
        val GAS_PRICE: BigInteger = BigInteger.valueOf(2_000_000_000)
    }
}