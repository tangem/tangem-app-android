package com.tangem.domain.jointaccount.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.jointaccount.network.JointNetworkReader
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.network.Network
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.MockAccounts
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IsJointOperationSlotBusyUseCaseTest {

    private val supplier: SingleJointAccountListSupplier = mockk()
    private val networkReader: JointNetworkReader = mockk()
    private val network: Network = mockk()

    private val useCase = IsJointOperationSlotBusyUseCase(supplier = supplier, networkReader = networkReader)

    private val account = MockAccounts.createJointAccount(ownerKeyIndex = 3)

    @BeforeEach
    fun resetMocks() {
        clearMocks(supplier, networkReader)
        coEvery { networkReader.getChainId(network = network) } returns CHAIN_ID.right()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN operation in the network WHEN invoke THEN slot is busy`(model: BusyModel) = runTest {
        // Arrange
        every { supplier(userWalletId = account.accountId.userWalletId) } returns flowOf(
            listOf(jointAccount(inFlightOperations = model.inFlightOperations)),
        )

        // Act
        val actual = useCase(account = account, network = network)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN account unknown to the supplier WHEN invoke THEN slot is free`() = runTest {
        // Arrange
        every { supplier(userWalletId = account.accountId.userWalletId) } returns flowOf(
            listOf(jointAccount(cryptoAccountId = "f".repeat(64))),
        )

        // Act
        val actual = useCase(account = account, network = network)

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN supplier emits nothing WHEN invoke THEN slot is free`() = runTest {
        // Arrange
        every { supplier(userWalletId = account.accountId.userWalletId) } returns emptyFlow()

        // Act
        val actual = useCase(account = account, network = network)

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN network without chain id WHEN invoke THEN slot is free`() = runTest {
        // Arrange
        coEvery { networkReader.getChainId(network = network) } returns IllegalArgumentException("no chain").left()
        every { supplier(userWalletId = account.accountId.userWalletId) } returns flowOf(listOf(jointAccount()))

        // Act
        val actual = useCase(account = account, network = network)

        // Assert
        assertThat(actual).isFalse()
    }

    data class BusyModel(val inFlightOperations: Map<Long, JointOperation>, val expected: Boolean)

    private fun provideTestModels() = listOf(
        BusyModel(inFlightOperations = emptyMap(), expected = false),
        BusyModel(inFlightOperations = mapOf(OTHER_CHAIN_ID to operation()), expected = false),
        BusyModel(inFlightOperations = mapOf(CHAIN_ID to operation()), expected = true),
        BusyModel(
            inFlightOperations = mapOf(CHAIN_ID to operation(status = JointOperation.Status.PENDING)),
            expected = true,
        ),
        BusyModel(
            inFlightOperations = mapOf(CHAIN_ID to operation(status = JointOperation.Status.UNKNOWN)),
            expected = true,
        ),
    )

    private fun jointAccount(
        inFlightOperations: Map<Long, JointOperation> = mapOf(CHAIN_ID to operation()),
        cryptoAccountId: String = account.accountId.value.uppercase(),
    ): JointAccount = JointAccount(
        cryptoAccountId = cryptoAccountId,
        membersCount = 2,
        threshold = 2,
        safeAddress = SAFE_ADDRESS,
        status = JointAccount.Status.ACTIVE,
        members = listOf(JointAccount.Member(name = "Alice", address = ALICE, role = JointAccount.Role.CREATOR)),
        source = StatusSource.ACTUAL,
        inFlightOperations = inFlightOperations,
    )

    private fun operation(
        status: JointOperation.Status = JointOperation.Status.AWAITING_SIGNATURES,
    ): JointOperation = JointOperation(
        operationId = "op-1",
        chainId = CHAIN_ID,
        safeTx = SafeTx(
            to = ALICE,
            value = BigInteger.ONE,
            data = "0x",
            operation = SafeTx.OPERATION_CALL,
            safeTxGas = BigInteger("150000"),
            baseGas = BigInteger("110000"),
            gasPrice = BigInteger("60000000000"),
            gasToken = "0x0000000000000000000000000000000000000000",
            refundReceiver = "0x0000000000000000000000000000000000000000",
            nonce = BigInteger.ZERO,
        ),
        status = status,
        initiator = ALICE,
        signatures = emptyList(),
    )

    private companion object {
        const val CHAIN_ID = 1L
        const val OTHER_CHAIN_ID = 137L
        const val SAFE_ADDRESS = "0x5afe5afe5afe5afe5afe5afe5afe5afe5afe5afe"
        const val ALICE = "0xa11ce00000000000000000000000000000000001"
    }
}