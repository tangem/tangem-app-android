package com.tangem.domain.jointaccount.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.model.JointOperationRejectError
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.jointaccount.network.JointNetworkReader
import com.tangem.domain.jointaccount.repository.JointAccountRepository
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.network.Network
import com.tangem.test.mock.MockAccounts
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RejectJointOperationUseCaseTest {

    private val repository: JointAccountRepository = mockk()
    private val fetcher: SingleJointAccountListFetcher = mockk()
    private val supplier: SingleJointAccountListSupplier = mockk()
    private val networkReader: JointNetworkReader = mockk()
    private val network: Network = mockk()

    private val useCase = RejectJointOperationUseCase(
        repository = repository,
        fetcher = fetcher,
        supplier = supplier,
        networkReader = networkReader,
    )

    private val account = MockAccounts.createJointAccount(ownerKeyIndex = 3)

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository, fetcher, supplier, networkReader)
        coEvery { fetcher.invoke(params = any()) } returns Unit.right()
        coEvery { supplier.invoke(userWalletId = MockAccounts.userWalletId) } returns flowOf(listOf(jointAccount()))
        coEvery { networkReader.getChainId(network) } returns CHAIN_ID.right()
        coEvery { repository.rejectOperation(any(), any()) } returns Unit.right()
    }

    @Test
    fun `GIVEN awaiting operation WHEN invoke THEN the shown operation is sent and the list refreshed after`() = runTest {
        // Act
        val actual = useCase(account = account, network = network, operationId = OPERATION_ID)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerifyOrder {
            repository.rejectOperation(userWalletId = MockAccounts.userWalletId, operationId = OPERATION_ID)
            fetcher.invoke(params = SingleJointAccountListFetcher.Params(userWalletId = MockAccounts.userWalletId))
        }
    }

    @Test
    fun `GIVEN the slot holds another operation WHEN invoke THEN OperationNotFound and nothing is sent`() = runTest {
        // Act
        val actual = useCase(account = account, network = network, operationId = "another-operation")

        // Assert
        assertThat(actual).isEqualTo(JointOperationRejectError.OperationNotFound.left())
        coVerify(exactly = 0) { repository.rejectOperation(any(), any()) }
    }

    @Test
    fun `GIVEN operation already pending WHEN invoke THEN ThresholdReached`() = runTest {
        // Arrange
        coEvery { supplier.invoke(userWalletId = MockAccounts.userWalletId) } returns
            flowOf(listOf(jointAccount(operation = operation(status = JointOperation.Status.PENDING))))

        // Act
        val actual = useCase(account = account, network = network, operationId = OPERATION_ID)

        // Assert
        assertThat(actual).isEqualTo(JointOperationRejectError.ThresholdReached.left())
        coVerify(exactly = 0) { repository.rejectOperation(any(), any()) }
    }

    @Test
    fun `GIVEN operation status unknown WHEN invoke THEN Failed and nothing is sent`() = runTest {
        // Arrange
        coEvery { supplier.invoke(userWalletId = MockAccounts.userWalletId) } returns
            flowOf(listOf(jointAccount(operation = operation(status = JointOperation.Status.UNKNOWN))))

        // Act
        val actual = useCase(account = account, network = network, operationId = OPERATION_ID)

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(JointOperationRejectError.Failed::class.java)
        coVerify(exactly = 0) { repository.rejectOperation(any(), any()) }
    }

    @Test
    fun `GIVEN the backend refuses WHEN invoke THEN its error comes back and the list is not refreshed`() = runTest {
        // Arrange
        coEvery { repository.rejectOperation(any(), any()) } returns JointOperationRejectError.ThresholdReached.left()

        // Act
        val actual = useCase(account = account, network = network, operationId = OPERATION_ID)

        // Assert
        assertThat(actual).isEqualTo(JointOperationRejectError.ThresholdReached.left())
        coVerify(exactly = 0) { fetcher.invoke(params = any()) }
    }

    @Test
    fun `GIVEN the refresh fails WHEN invoke THEN the refusal still counts`() = runTest {
        // Arrange
        coEvery { fetcher.invoke(params = any()) } returns IllegalStateException("offline").left()

        // Act
        val actual = useCase(account = account, network = network, operationId = OPERATION_ID)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
    }

    private fun jointAccount(operation: JointOperation = operation()): JointAccount = JointAccount(
        cryptoAccountId = account.accountId.value,
        membersCount = 2,
        threshold = 2,
        safeAddress = SAFE_ADDRESS,
        status = JointAccount.Status.ACTIVE,
        members = listOf(
            JointAccount.Member(name = "Alice", address = ALICE, role = JointAccount.Role.CREATOR),
        ),
        source = StatusSource.ACTUAL,
        inFlightOperations = mapOf(CHAIN_ID to operation),
    )

    private fun operation(
        status: JointOperation.Status = JointOperation.Status.AWAITING_SIGNATURES,
    ): JointOperation = JointOperation(
        operationId = OPERATION_ID,
        chainId = CHAIN_ID,
        safeTx = SafeTx(
            to = RECIPIENT,
            value = BigInteger("1000000000000000000"),
            data = "0x",
            operation = SafeTx.OPERATION_CALL,
            safeTxGas = BigInteger("150000"),
            baseGas = BigInteger("110000"),
            gasPrice = BigInteger("60000000000"),
            gasToken = "0x0000000000000000000000000000000000000000",
            refundReceiver = "0xfee0000000000000000000000000000000000001",
            nonce = BigInteger.ZERO,
        ),
        status = status,
        initiator = ALICE,
        signatures = emptyList(),
    )

    private companion object {
        const val ALICE = "0x7e5f4552091a69125d5dfcb7b8c2659029395bdf"
        const val RECIPIENT = "0x1111111111111111111111111111111111111111"
        const val SAFE_ADDRESS = "0x5afe000000000000000000000000000000000001"
        const val OPERATION_ID = "3f1a2b4c-5d6e-7f80-91a2-b3c4d5e6f708"
        const val CHAIN_ID = 137L
    }
}