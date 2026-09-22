package com.tangem.data.jointaccount.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.domain.jointaccount.gas.SafeTxGasBuilder
import com.tangem.data.jointaccount.safe.DefaultJointTransferPayloadCodec
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.model.JointOperationCreateError
import com.tangem.domain.jointaccount.model.JointOperationSigningError
import com.tangem.domain.jointaccount.model.JointTransferIntent
import com.tangem.domain.jointaccount.model.SafeInnerCall
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.jointaccount.network.JointNetworkReader
import com.tangem.domain.jointaccount.repository.JointAccountRepository
import com.tangem.domain.jointaccount.safe.SafeCompositionChecker
import com.tangem.domain.jointaccount.safe.SafeDeployment
import com.tangem.domain.jointaccount.signing.JointOperationSigner
import com.tangem.domain.jointaccount.usecase.CreateJointOperationUseCase
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.network.Network
import com.tangem.test.mock.MockAccounts
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CreateJointOperationUseCaseTest {

    private val repository: JointAccountRepository = mockk()
    private val signer: JointOperationSigner = mockk()
    private val fetcher: SingleJointAccountListFetcher = mockk()
    private val supplier: SingleJointAccountListSupplier = mockk()
    private val gasBuilder: SafeTxGasBuilder = mockk()
    private val compositionChecker: SafeCompositionChecker = mockk()
    private val networkReader: JointNetworkReader = mockk()
    private val network: Network = mockk()

    private val useCase = CreateJointOperationUseCase(
        repository = repository,
        signer = signer,
        fetcher = fetcher,
        supplier = supplier,
        gasBuilder = gasBuilder,
        compositionChecker = compositionChecker,
        networkReader = networkReader,
        payloadCodec = DefaultJointTransferPayloadCodec(),
    )

    private val account = MockAccounts.createJointAccount(ownerKeyIndex = 3)
    private val intent = JointTransferIntent.Native(recipient = RECIPIENT, amountWei = AMOUNT)

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository, signer, fetcher, supplier, gasBuilder, compositionChecker, networkReader)
        coEvery { fetcher.invoke(params = any()) } returns Unit.right()
        coEvery { supplier.invoke(userWalletId = MockAccounts.userWalletId) } returns
            flowOf(listOf(jointAccount()))
        coEvery { networkReader.getChainId(network) } returns CHAIN_ID.right()
        coEvery { compositionChecker.ensureMatches(any(), any(), any(), any(), any()) } returns
            SafeDeployment.DEPLOYED.right()
        coEvery { gasBuilder.build(userWalletId = any(), network = any(), call = any()) } returns safeTx().right()
        coEvery { signer.signOperation(any(), any(), any(), any(), any()) } returns SIGNATURE.right()
        coEvery { repository.createOperation(any(), any(), any(), any(), any()) } returns Unit.right()
    }

    @Test
    fun `GIVEN active account WHEN invoke THEN gate check build sign create fetch in order`() = runTest {
        // Arrange
        val callSlot = slot<SafeInnerCall>()
        coEvery { gasBuilder.build(userWalletId = any(), network = any(), call = capture(callSlot)) } returns
            safeTx().right()

        // Act
        val actual = useCase(account = account, network = network, intent = intent)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        assertThat(callSlot.captured).isEqualTo(
            SafeInnerCall(to = RECIPIENT.lowercase(), value = AMOUNT, data = "0x"),
        )
        coVerifyOrder {
            fetcher.invoke(params = SingleJointAccountListFetcher.Params(MockAccounts.userWalletId))
            compositionChecker.ensureMatches(
                userWalletId = MockAccounts.userWalletId,
                network = network,
                safeAddress = SAFE_ADDRESS,
                owners = listOf(ALICE, BOB),
                threshold = 2,
            )
            gasBuilder.build(userWalletId = MockAccounts.userWalletId, network = network, call = any())
            signer.signOperation(
                userWalletId = MockAccounts.userWalletId,
                ownerKeyIndex = account.ownerKeyIndex,
                safeTx = safeTx(),
                chainId = CHAIN_ID,
                safeAddress = SAFE_ADDRESS,
            )
            repository.createOperation(
                userWalletId = MockAccounts.userWalletId,
                safeAddress = SAFE_ADDRESS,
                chainId = CHAIN_ID,
                safeTx = safeTx(),
                signature = SIGNATURE,
            )
            fetcher.invoke(params = SingleJointAccountListFetcher.Params(MockAccounts.userWalletId))
        }
    }

    @Test
    fun `GIVEN erc20 intent WHEN invoke THEN the inner call carries transfer calldata and zero value`() = runTest {
        // Arrange
        val callSlot = slot<SafeInnerCall>()
        coEvery { gasBuilder.build(userWalletId = any(), network = any(), call = capture(callSlot)) } returns
            safeTx().right()

        // Act
        val actual = useCase(
            account = account,
            network = network,
            intent = JointTransferIntent.Erc20(tokenContract = TOKEN, recipient = RECIPIENT, amountWei = AMOUNT),
        )

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        val captured = callSlot.captured
        assertThat(captured.to).isEqualTo(TOKEN.lowercase())
        assertThat(captured.value).isEqualTo(BigInteger.ZERO)
        assertThat(captured.data).startsWith("0xa9059cbb")
    }

    @Test
    fun `GIVEN slot busy WHEN invoke THEN SlotBusy and nothing is built or signed`() = runTest {
        // Arrange
        coEvery { supplier.invoke(userWalletId = MockAccounts.userWalletId) } returns
            flowOf(listOf(jointAccount(inFlightOperations = mapOf(CHAIN_ID to jointOperation()))))

        // Act
        val actual = useCase(account = account, network = network, intent = intent)

        // Assert
        assertThat(actual).isEqualTo(JointOperationCreateError.SlotBusy.left())
        coVerify(exactly = 0) { gasBuilder.build(any(), any(), any()) }
        coVerify(exactly = 0) { signer.signOperation(any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { repository.createOperation(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN account not active WHEN invoke THEN AccountNotReady`() = runTest {
        // Arrange
        coEvery { supplier.invoke(userWalletId = MockAccounts.userWalletId) } returns
            flowOf(listOf(jointAccount(status = JointAccount.Status.CONFIRMING)))

        // Act
        val actual = useCase(account = account, network = network, intent = intent)

        // Assert
        assertThat(actual).isEqualTo(JointOperationCreateError.AccountNotReady.left())
        coVerify(exactly = 0) { signer.signOperation(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN composition mismatch WHEN invoke THEN blocked before anything is signed`() = runTest {
        // Arrange
        coEvery { compositionChecker.ensureMatches(any(), any(), any(), any(), any()) } returns
            JointOperationCreateError.CompositionMismatch.left()

        // Act
        val actual = useCase(account = account, network = network, intent = intent)

        // Assert
        assertThat(actual).isEqualTo(JointOperationCreateError.CompositionMismatch.left())
        coVerify(exactly = 0) { signer.signOperation(any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { repository.createOperation(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN recipient is the safe itself WHEN invoke THEN InvalidPayload and nothing signed`() = runTest {
        // Act
        val actual = useCase(
            account = account,
            network = network,
            intent = JointTransferIntent.Native(recipient = SAFE_ADDRESS.uppercase().replace("0X", "0x"), amountWei = AMOUNT),
        )

        // Assert
        assertThat(actual).isEqualTo(JointOperationCreateError.InvalidPayload.left())
        coVerify(exactly = 0) { signer.signOperation(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN amount beyond uint256 WHEN invoke THEN InvalidPayload instead of a crash`() = runTest {
        // Act
        val actual = useCase(
            account = account,
            network = network,
            intent = JointTransferIntent.Erc20(
                tokenContract = TOKEN,
                recipient = RECIPIENT,
                amountWei = BigInteger.ONE.shiftLeft(256),
            ),
        )

        // Assert
        assertThat(actual).isEqualTo(JointOperationCreateError.InvalidPayload.left())
        coVerify(exactly = 0) { signer.signOperation(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN zero amount WHEN invoke THEN InvalidPayload`() = runTest {
        // Act
        val actual = useCase(
            account = account,
            network = network,
            intent = JointTransferIntent.Native(recipient = RECIPIENT, amountWei = BigInteger.ZERO),
        )

        // Assert
        assertThat(actual).isEqualTo(JointOperationCreateError.InvalidPayload.left())
        coVerify(exactly = 0) { signer.signOperation(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN network moved under first attempt WHEN invoke THEN rebuilt and honestly re-signed`() = runTest {
        // Arrange
        coEvery { repository.createOperation(any(), any(), any(), any(), any()) } returnsMany listOf(
            JointOperationCreateError.NetworkStateMoved.left(),
            Unit.right(),
        )

        // Act
        val actual = useCase(account = account, network = network, intent = intent)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        // Every signature request is preceded by its own composition check
        coVerify(exactly = 2) { compositionChecker.ensureMatches(any(), any(), any(), any(), any()) }
        coVerify(exactly = 2) { gasBuilder.build(any(), any(), any()) }
        coVerify(exactly = 2) { signer.signOperation(any(), any(), any(), any(), any()) }
        coVerify(exactly = 2) { repository.createOperation(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN network moved twice WHEN invoke THEN NetworkStateMoved without a third tap`() = runTest {
        // Arrange
        coEvery { repository.createOperation(any(), any(), any(), any(), any()) } returns
            JointOperationCreateError.NetworkStateMoved.left()

        // Act
        val actual = useCase(account = account, network = network, intent = intent)

        // Assert
        assertThat(actual).isEqualTo(JointOperationCreateError.NetworkStateMoved.left())
        coVerify(exactly = 2) { signer.signOperation(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN user cancels the tap WHEN invoke THEN UserCancelled and nothing is sent`() = runTest {
        // Arrange
        coEvery { signer.signOperation(any(), any(), any(), any(), any()) } returns
            JointOperationSigningError.UserCancelled.left()

        // Act
        val actual = useCase(account = account, network = network, intent = intent)

        // Assert
        assertThat(actual).isEqualTo(JointOperationCreateError.UserCancelled.left())
        coVerify(exactly = 0) { repository.createOperation(any(), any(), any(), any(), any()) }
    }

    private fun jointAccount(
        status: JointAccount.Status = JointAccount.Status.ACTIVE,
        inFlightOperations: Map<Long, JointOperation> = emptyMap(),
    ): JointAccount = JointAccount(
        cryptoAccountId = account.accountId.value,
        membersCount = 2,
        threshold = 2,
        safeAddress = SAFE_ADDRESS,
        status = status,
        members = listOf(
            JointAccount.Member(name = "Alice", address = ALICE, role = JointAccount.Role.CREATOR),
            JointAccount.Member(name = "Bob", address = BOB, role = JointAccount.Role.MEMBER),
        ),
        source = StatusSource.ACTUAL,
        inFlightOperations = inFlightOperations,
    )

    private fun jointOperation(): JointOperation = JointOperation(
        operationId = "3f1a2b4c-5d6e-7f80-91a2-b3c4d5e6f708",
        chainId = CHAIN_ID,
        safeTx = safeTx(),
        status = JointOperation.Status.AWAITING_SIGNATURES,
        initiator = ALICE,
        signatures = emptyList(),
    )

    private fun safeTx(): SafeTx = SafeTx(
        to = RECIPIENT.lowercase(),
        value = AMOUNT,
        data = "0x",
        operation = SafeTx.OPERATION_CALL,
        safeTxGas = BigInteger.valueOf(150_000),
        baseGas = BigInteger.valueOf(110_000),
        gasPrice = BigInteger.valueOf(60_000_000_000),
        gasToken = "0x0000000000000000000000000000000000000000",
        refundReceiver = "0x0000000000000000000000000000000000000000",
        nonce = BigInteger.valueOf(4),
    )

    private companion object {
        const val ALICE = "0x7e5f4552091a69125d5dfcb7b8c2659029395bdf"
        const val BOB = "0x2b5ad5c4795c026514f8317c7a215e218dccd6cf"
        const val RECIPIENT = "0x1111111111111111111111111111111111111111"
        const val TOKEN = "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48"
        const val SAFE_ADDRESS = "0x5afe000000000000000000000000000000000001"
        const val SIGNATURE = "0xdeadbeef"
        const val CHAIN_ID = 137L
        val AMOUNT: BigInteger = BigInteger("1000000000000000000")
    }
}