package com.tangem.domain.jointaccount.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.model.JointOperationSlot
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.jointaccount.network.JointNetworkReader
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.network.Network
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.MockAccounts
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetJointOperationSlotUseCaseTest {

    private val supplier: SingleJointAccountListSupplier = mockk()
    private val fetcher: SingleJointAccountListFetcher = mockk()
    private val networkReader: JointNetworkReader = mockk()
    private val network: Network = mockk()

    private val useCase = GetJointOperationSlotUseCase(
        supplier = supplier,
        fetcher = fetcher,
        networkReader = networkReader,
    )

    private val account = MockAccounts.createJointAccount(ownerKeyIndex = 3)
    private val fetcherParams = SingleJointAccountListFetcher.Params(userWalletId = account.accountId.userWalletId)

    @BeforeEach
    fun resetMocks() {
        clearMocks(supplier, fetcher, networkReader)
        coEvery { networkReader.getChainId(network = network) } returns CHAIN_ID.right()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN actual account WHEN invoke THEN slot follows its operations`(model: SlotModel) = runTest {
        // Arrange
        every { supplier(userWalletId = account.accountId.userWalletId) } returns flowOf(
            listOf(jointAccount(inFlightOperations = model.inFlightOperations)),
        )

        // Act
        val actual = useCase(account = account, network = network)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
        coVerify(exactly = 0) { fetcher(params = any()) }
    }

    @Test
    fun `GIVEN stale account and fetch succeeds WHEN invoke THEN slot follows the fetched account`() = runTest {
        // Arrange
        every { supplier(userWalletId = account.accountId.userWalletId) } returnsMany listOf(
            flowOf(listOf(jointAccount(inFlightOperations = emptyMap(), source = StatusSource.ONLY_CACHE))),
            flowOf(listOf(jointAccount())),
        )
        coEvery { fetcher(params = fetcherParams) } returns Unit.right()

        // Act
        val actual = useCase(account = account, network = network)

        // Assert
        assertThat(actual).isEqualTo(JointOperationSlot.BUSY)
    }

    @ParameterizedTest
    @EnumSource(value = StatusSource::class, names = ["CACHE", "ONLY_CACHE"])
    fun `GIVEN stale account and fetch fails WHEN invoke THEN slot is unknown`(source: StatusSource) = runTest {
        // Arrange
        every { supplier(userWalletId = account.accountId.userWalletId) } returns flowOf(
            listOf(jointAccount(inFlightOperations = emptyMap(), source = source)),
        )
        coEvery { fetcher(params = fetcherParams) } returns IllegalStateException("503").left()

        // Act
        val actual = useCase(account = account, network = network)

        // Assert
        assertThat(actual).isEqualTo(JointOperationSlot.UNKNOWN)
    }

    @Test
    fun `GIVEN supplier emits nothing and fetch fails WHEN invoke THEN slot is unknown`() = runTest {
        // Arrange
        every { supplier(userWalletId = account.accountId.userWalletId) } returns emptyFlow()
        coEvery { fetcher(params = fetcherParams) } returns IllegalStateException("503").left()

        // Act
        val actual = useCase(account = account, network = network)

        // Assert
        assertThat(actual).isEqualTo(JointOperationSlot.UNKNOWN)
    }

    @Test
    fun `GIVEN account unknown to the backend WHEN invoke THEN slot is unknown`() = runTest {
        // Arrange
        every { supplier(userWalletId = account.accountId.userWalletId) } returns flowOf(
            listOf(jointAccount(cryptoAccountId = "f".repeat(64))),
        )
        coEvery { fetcher(params = fetcherParams) } returns Unit.right()

        // Act
        val actual = useCase(account = account, network = network)

        // Assert
        assertThat(actual).isEqualTo(JointOperationSlot.UNKNOWN)
    }

    @Test
    fun `GIVEN network without chain id WHEN invoke THEN slot is unknown`() = runTest {
        // Arrange
        coEvery { networkReader.getChainId(network = network) } returns IllegalArgumentException("no chain").left()
        every { supplier(userWalletId = account.accountId.userWalletId) } returns flowOf(listOf(jointAccount()))

        // Act
        val actual = useCase(account = account, network = network)

        // Assert
        assertThat(actual).isEqualTo(JointOperationSlot.UNKNOWN)
    }

    data class SlotModel(val inFlightOperations: Map<Long, JointOperation>, val expected: JointOperationSlot)

    private fun provideTestModels() = listOf(
        SlotModel(inFlightOperations = emptyMap(), expected = JointOperationSlot.FREE),
        SlotModel(inFlightOperations = mapOf(OTHER_CHAIN_ID to operation()), expected = JointOperationSlot.FREE),
        SlotModel(inFlightOperations = mapOf(CHAIN_ID to operation()), expected = JointOperationSlot.BUSY),
        SlotModel(
            inFlightOperations = mapOf(CHAIN_ID to operation(status = JointOperation.Status.PENDING)),
            expected = JointOperationSlot.BUSY,
        ),
        SlotModel(
            inFlightOperations = mapOf(CHAIN_ID to operation(status = JointOperation.Status.UNKNOWN)),
            expected = JointOperationSlot.BUSY,
        ),
    )

    private fun jointAccount(
        inFlightOperations: Map<Long, JointOperation> = mapOf(CHAIN_ID to operation()),
        cryptoAccountId: String = account.accountId.value.uppercase(),
        source: StatusSource = StatusSource.ACTUAL,
    ): JointAccount = JointAccount(
        cryptoAccountId = cryptoAccountId,
        membersCount = 2,
        threshold = 2,
        safeAddress = SAFE_ADDRESS,
        status = JointAccount.Status.ACTIVE,
        members = listOf(JointAccount.Member(name = "Alice", address = ALICE, role = JointAccount.Role.CREATOR)),
        source = source,
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