package com.tangem.data.jointaccount.usecase

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.data.jointaccount.safe.DefaultJointTransferPayloadCodec
import com.tangem.domain.jointaccount.address.JointOwnerAddressResolver
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.model.JointOperationSignerState.Answer
import com.tangem.domain.jointaccount.model.JointTransferIntent
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.jointaccount.network.JointNetworkReader
import com.tangem.domain.jointaccount.safe.JointOperationSignersResolver
import com.tangem.domain.jointaccount.safe.JointOperationTransferResolver
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.jointaccount.usecase.GetPendingJointOperationsUseCase
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.test.mock.MockAccounts
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetPendingJointOperationsUseCaseTest {

    private val supplier: SingleJointAccountListSupplier = mockk()
    private val ownerAddressResolver: JointOwnerAddressResolver = mockk()
    private val networkReader: JointNetworkReader = mockk()
    private val network: Network = mockk()
    private val coin: CryptoCurrency.Coin = mockk {
        every { network } returns this@GetPendingJointOperationsUseCaseTest.network
    }

    private val useCase = GetPendingJointOperationsUseCase(
        jointAccountListSupplier = supplier,
        transferResolver = JointOperationTransferResolver(payloadCodec = DefaultJointTransferPayloadCodec()),
        signersResolver = JointOperationSignersResolver(),
        ownerAddressResolver = ownerAddressResolver,
        networkReader = networkReader,
    )

    private val account: Account.Joint = MockAccounts
        .createJointAccount(ownerKeyIndex = 0, cryptoCurrencies = listOf(coin))

    @BeforeEach
    fun resetMocks() {
        clearMocks(supplier, ownerAddressResolver, networkReader)
        coEvery { supplier.invoke(userWalletId = MockAccounts.userWalletId) } returns flowOf(listOf(jointAccount()))
        coEvery { networkReader.getChainId(network) } returns CHAIN_ID.right()
        coEvery { ownerAddressResolver.resolve(any(), any()) } returns BOB.right()
    }

    @Test
    fun `GIVEN an operation this member has not answered WHEN invoke THEN it is announced`() = runTest {
        // Act
        val actual = invoke()

        // Assert
        assertThat(actual).hasSize(1)
        val operation = actual.first()
        assertThat(operation.operationId).isEqualTo(OPERATION_ID)
        assertThat(operation.initiatorName).isEqualTo("Alice")
        assertThat(operation.isOwn).isFalse()
        assertThat(operation.ownAnswer).isEqualTo(Answer.WAITING)
        assertThat(operation.collected).isEqualTo(1)
        assertThat(operation.threshold).isEqualTo(THRESHOLD)
        assertThat(operation.transfer.currency).isEqualTo(coin)
        assertThat(operation.transfer.intent)
            .isEqualTo(JointTransferIntent.Native(recipient = RECIPIENT, amountWei = AMOUNT))
    }

    @Test
    fun `GIVEN this member declined WHEN invoke THEN the operation is not announced`() = runTest {
        // Arrange
        coEvery { supplier.invoke(userWalletId = MockAccounts.userWalletId) } returns flowOf(
            listOf(
                jointAccount(
                    signatures = listOf(
                        signature(address = ALICE, state = JointOperation.MemberSignature.State.SIGNED),
                        signature(address = BOB, state = JointOperation.MemberSignature.State.DECLINED),
                    ),
                ),
            ),
        )

        // Act
        val actual = invoke()

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN this member created the operation WHEN invoke THEN it is announced as their own`() = runTest {
        // Arrange
        coEvery { ownerAddressResolver.resolve(any(), any()) } returns ALICE.right()

        // Act
        val actual = invoke()

        // Assert
        assertThat(actual.single().isOwn).isTrue()
        assertThat(actual.single().ownAnswer).isEqualTo(Answer.CREATED)
    }

    @Test
    fun `GIVEN the threshold is already collected and the operation executes WHEN invoke THEN nothing is announced`() =
        runTest {
            // Arrange
            coEvery { supplier.invoke(userWalletId = MockAccounts.userWalletId) } returns flowOf(
                listOf(jointAccount(status = JointOperation.Status.PENDING)),
            )

            // Act
            val actual = invoke()

            // Assert
            assertThat(actual).isEmpty()
        }

    @Test
    fun `GIVEN the operation lives in a chain the account does not hold WHEN invoke THEN nothing is announced`() =
        runTest {
            // Arrange
            coEvery { supplier.invoke(userWalletId = MockAccounts.userWalletId) } returns flowOf(
                listOf(jointAccount(chainId = OTHER_CHAIN_ID)),
            )

            // Act
            val actual = invoke()

            // Assert
            assertThat(actual).isEmpty()
        }

    @Test
    fun `GIVEN no joint account matches the roster WHEN invoke THEN nothing is announced`() = runTest {
        // Arrange
        coEvery { supplier.invoke(userWalletId = MockAccounts.userWalletId) } returns flowOf(
            listOf(jointAccount(cryptoAccountId = "ff".repeat(n = 32))),
        )

        // Act
        val actual = invoke()

        // Assert
        assertThat(actual).isEmpty()
    }

    private suspend fun invoke() = useCase(
        userWalletId = MockAccounts.userWalletId,
        accounts = listOf(account),
    ).first()

    private fun jointAccount(
        cryptoAccountId: String = account.accountId.value,
        chainId: Long = CHAIN_ID,
        status: JointOperation.Status = JointOperation.Status.AWAITING_SIGNATURES,
        signatures: List<JointOperation.MemberSignature> = listOf(
            signature(address = ALICE, state = JointOperation.MemberSignature.State.SIGNED),
        ),
    ) = JointAccount(
        cryptoAccountId = cryptoAccountId,
        membersCount = 5,
        threshold = THRESHOLD,
        safeAddress = SAFE_ADDRESS,
        status = JointAccount.Status.ACTIVE,
        members = listOf(
            JointAccount.Member(name = "Alice", address = ALICE, role = JointAccount.Role.CREATOR),
            JointAccount.Member(name = "Bob", address = BOB, role = JointAccount.Role.MEMBER),
        ),
        source = StatusSource.ACTUAL,
        inFlightOperations = mapOf(
            chainId to JointOperation(
                operationId = OPERATION_ID,
                chainId = chainId,
                safeTx = safeTx(),
                status = status,
                initiator = ALICE,
                signatures = signatures,
            ),
        ),
    )

    private fun signature(address: String, state: JointOperation.MemberSignature.State) =
        JointOperation.MemberSignature(address = address, state = state, at = "2026-09-01T12:00:00Z")

    private fun safeTx() = SafeTx(
        to = RECIPIENT,
        value = AMOUNT,
        data = "0x",
        operation = SafeTx.OPERATION_CALL,
        safeTxGas = BigInteger.valueOf(21_000),
        baseGas = BigInteger.valueOf(110_000),
        gasPrice = BigInteger.ONE,
        gasToken = ZERO_ADDRESS,
        refundReceiver = ZERO_ADDRESS,
        nonce = BigInteger.ZERO,
    )

    private companion object {
        const val CHAIN_ID = 1L
        const val OTHER_CHAIN_ID = 137L
        const val THRESHOLD = 3
        const val OPERATION_ID = "27ca1f4e-0f2e-4a1e-9d0b-3b9f6a1c2d3e"
        const val SAFE_ADDRESS = "0x1111111111111111111111111111111111111111"
        const val RECIPIENT = "0x2222222222222222222222222222222222222222"
        const val ALICE = "0x3333333333333333333333333333333333333333"
        const val BOB = "0x4444444444444444444444444444444444444444"
        const val ZERO_ADDRESS = "0x0000000000000000000000000000000000000000"
        val AMOUNT: BigInteger = BigInteger("500000000000000000")
    }
}