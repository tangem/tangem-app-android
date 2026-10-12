package com.tangem.domain.jointaccount.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.jointaccount.address.JointOwnerAddressResolver
import com.tangem.domain.jointaccount.blockaid.JointOperationBlockaidGate
import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.model.JointOperationCreateError
import com.tangem.domain.jointaccount.model.JointOperationSignError
import com.tangem.domain.jointaccount.model.JointTransferIntent
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.jointaccount.gas.JointRefundReceiverProvider
import com.tangem.domain.jointaccount.network.JointNetworkReader
import com.tangem.domain.jointaccount.safe.SafeCompositionChecker
import com.tangem.domain.jointaccount.safe.SafeDeployment
import com.tangem.domain.jointaccount.repository.JointAccountRepository
import com.tangem.domain.jointaccount.signing.JointOperationSigner
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.jointaccount.verifier.JointOperationVerifier
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
internal class SignJointOperationUseCaseTest {

    private val repository: JointAccountRepository = mockk()
    private val signer: JointOperationSigner = mockk()
    private val fetcher: SingleJointAccountListFetcher = mockk()
    private val supplier: SingleJointAccountListSupplier = mockk()
    private val verifier: JointOperationVerifier = mockk()
    private val blockaidGate: JointOperationBlockaidGate = mockk()
    private val ownerAddressResolver: JointOwnerAddressResolver = mockk()
    private val networkReader: JointNetworkReader = mockk()
    private val compositionChecker: SafeCompositionChecker = mockk()
    private val refundReceiverProvider: JointRefundReceiverProvider = mockk()
    private val network: Network = mockk()

    private val useCase = SignJointOperationUseCase(
        repository = repository,
        signer = signer,
        fetcher = fetcher,
        supplier = supplier,
        verifier = verifier,
        blockaidGate = blockaidGate,
        ownerAddressResolver = ownerAddressResolver,
        networkReader = networkReader,
        compositionChecker = compositionChecker,
        refundReceiverProvider = refundReceiverProvider,
    )

    private val account = MockAccounts.createJointAccount(ownerKeyIndex = 3)
    private val displayed = JointTransferIntent.Native(recipient = RECIPIENT, amountWei = AMOUNT)

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            repository,
            signer,
            fetcher,
            supplier,
            verifier,
            blockaidGate,
            ownerAddressResolver,
            networkReader,
            compositionChecker,
            refundReceiverProvider,
        )
        coEvery { fetcher.invoke(params = any()) } returns Unit.right()
        coEvery { supplier.invoke(userWalletId = MockAccounts.userWalletId) } returns
            flowOf(listOf(jointAccount()))
        coEvery { networkReader.getChainId(network) } returns CHAIN_ID.right()
        coEvery { verifier.verify(any(), any(), any(), any(), any()) } returns Unit.right()
        coEvery { networkReader.getGasPrice(any(), any()) } returns GAS_PRICE.right()
        coEvery { refundReceiverProvider.refundReceiver() } returns REFUND_RECEIVER
        coEvery { compositionChecker.ensureMatches(any(), any(), any(), any(), any()) } returns
            SafeDeployment.DEPLOYED.right()
        coEvery { ownerAddressResolver.resolve(any(), any()) } returns ALICE.right()
        coEvery { blockaidGate.verify(any(), any(), any(), any(), any()) } returns
            JointOperationBlockaidGate.Verdict.SAFE
        coEvery { signer.signOperation(any(), any(), any(), any(), any()) } returns SIGNATURE.right()
        coEvery { repository.signOperation(any(), any(), any(), any()) } returns Unit.right()
    }

    @Test
    fun `GIVEN the slot holds another operation WHEN invoke THEN nothing is signed`() = runTest {
        // Act
        val actual = useCase(
            account = account,
            network = network,
            operationId = "another-operation",
            displayed = displayed,
        )

        // Assert
        assertThat(actual).isEqualTo(JointOperationSignError.OperationNotFound.left())
        coVerify(exactly = 0) { signer.signOperation(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN awaiting operation WHEN invoke THEN verify gate sign send fetch in order`() = runTest {
        // Act
        val actual = useCase(account = account, network = network, operationId = OPERATION_ID, displayed = displayed)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerifyOrder {
            verifier.verify(
                jointAccount = jointAccount(),
                operation = operation(),
                displayed = displayed,
                ownerAddress = ALICE,
                refundBounds = JointOperationVerifier.RefundBounds(
                    refundReceiver = REFUND_RECEIVER,
                    networkGasPrice = GAS_PRICE,
                ),
            )
            blockaidGate.verify(
                network = network,
                safeTx = operation().safeTx,
                chainId = CHAIN_ID,
                safeAddress = SAFE_ADDRESS,
                ownerAddress = ALICE,
            )
            signer.signOperation(
                userWalletId = MockAccounts.userWalletId,
                ownerKeyIndex = account.ownerKeyIndex,
                safeTx = operation().safeTx,
                chainId = CHAIN_ID,
                safeAddress = SAFE_ADDRESS,
            )
            repository.signOperation(
                userWalletId = MockAccounts.userWalletId,
                operationId = OPERATION_ID,
                safeTx = operation().safeTx,
                signature = SIGNATURE,
            )
            fetcher.invoke(params = SingleJointAccountListFetcher.Params(MockAccounts.userWalletId))
        }
    }

    @Test
    fun `GIVEN verifier blocks WHEN invoke THEN the signer is never reached`() = runTest {
        // Arrange
        val reason = JointOperationSignError.VerificationFailed(
            reason = JointOperationSignError.VerificationFailed.Reason.PAYLOAD_MISMATCH,
        )
        coEvery { verifier.verify(any(), any(), any(), any(), any()) } returns reason.left()

        // Act
        val actual = useCase(account = account, network = network, operationId = OPERATION_ID, displayed = displayed)

        // Assert
        assertThat(actual).isEqualTo(reason.left())
        coVerify(exactly = 0) { signer.signOperation(any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { repository.signOperation(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN blockaid says malicious WHEN invoke THEN the signer is never reached`() = runTest {
        // Arrange
        coEvery { blockaidGate.verify(any(), any(), any(), any(), any()) } returns
            JointOperationBlockaidGate.Verdict.MALICIOUS

        // Act
        val actual = useCase(account = account, network = network, operationId = OPERATION_ID, displayed = displayed)

        // Assert
        assertThat(actual).isEqualTo(JointOperationSignError.Malicious.left())
        coVerify(exactly = 0) { signer.signOperation(any(), any(), any(), any(), any()) }
    }

    /** The local checks pass on the composition the app knows; the deployed Safe is the second opinion */
    @Test
    fun `GIVEN the deployed safe answers another composition WHEN invoke THEN nothing is signed`() = runTest {
        // Arrange
        coEvery { compositionChecker.ensureMatches(any(), any(), any(), any(), any()) } returns
            JointOperationCreateError.CompositionMismatch.left()

        // Act
        val actual = useCase(account = account, network = network, operationId = OPERATION_ID, displayed = displayed)

        // Assert
        assertThat(actual).isEqualTo(
            JointOperationSignError.VerificationFailed(
                reason = JointOperationSignError.VerificationFailed.Reason.COMPOSITION_MISMATCH,
            ).left(),
        )
        coVerify(exactly = 0) { signer.signOperation(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN the composition is checked WHEN invoke THEN it is checked against the account the app knows`() =
        runTest {
            // Act
            useCase(account = account, network = network, operationId = OPERATION_ID, displayed = displayed)

            // Assert
            coVerify(exactly = 1) {
                compositionChecker.ensureMatches(
                    userWalletId = MockAccounts.userWalletId,
                    network = network,
                    safeAddress = SAFE_ADDRESS,
                    owners = jointAccount().members.map { it.address },
                    threshold = jointAccount().threshold,
                )
            }
        }

    @Test
    fun `GIVEN no verdict for a safe not deployed yet WHEN invoke THEN the operation is still signed`() = runTest {
        // Arrange
        coEvery { compositionChecker.ensureMatches(any(), any(), any(), any(), any()) } returns
            SafeDeployment.NOT_DEPLOYED.right()
        coEvery { blockaidGate.verify(any(), any(), any(), any(), any()) } returns
            JointOperationBlockaidGate.Verdict.UNKNOWN

        // Act
        val actual = useCase(account = account, network = network, operationId = OPERATION_ID, displayed = displayed)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 1) { signer.signOperation(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN blockaid unavailable for a safe not deployed yet WHEN invoke THEN nothing is signed`() = runTest {
        // Arrange
        coEvery { compositionChecker.ensureMatches(any(), any(), any(), any(), any()) } returns
            SafeDeployment.NOT_DEPLOYED.right()
        coEvery { blockaidGate.verify(any(), any(), any(), any(), any()) } returns
            JointOperationBlockaidGate.Verdict.UNAVAILABLE

        // Act
        val actual = useCase(account = account, network = network, operationId = OPERATION_ID, displayed = displayed)

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(JointOperationSignError.Failed::class.java)
        coVerify(exactly = 0) { signer.signOperation(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN no verdict for a deployed safe WHEN invoke THEN nothing is signed`() = runTest {
        // Arrange
        coEvery { blockaidGate.verify(any(), any(), any(), any(), any()) } returns
            JointOperationBlockaidGate.Verdict.UNKNOWN

        // Act
        val actual = useCase(account = account, network = network, operationId = OPERATION_ID, displayed = displayed)

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(JointOperationSignError.Failed::class.java)
        coVerify(exactly = 0) { signer.signOperation(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN no safe address WHEN invoke THEN the safe address check fails before anything is signed`() = runTest {
        // Arrange
        coEvery { supplier.invoke(userWalletId = MockAccounts.userWalletId) } returns
            flowOf(listOf(jointAccount(safeAddress = null)))

        // Act
        val actual = useCase(account = account, network = network, operationId = OPERATION_ID, displayed = displayed)

        // Assert
        val expected = JointOperationSignError.VerificationFailed(
            reason = JointOperationSignError.VerificationFailed.Reason.SAFE_ADDRESS_MISMATCH,
        )
        assertThat(actual).isEqualTo(expected.left())
        coVerify(exactly = 0) { signer.signOperation(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN owner key not derived WHEN invoke THEN verification fails before the verifier runs`() = runTest {
        // Arrange
        coEvery { ownerAddressResolver.resolve(any(), any()) } returns null.right()

        // Act
        val actual = useCase(account = account, network = network, operationId = OPERATION_ID, displayed = displayed)

        // Assert
        val expected = JointOperationSignError.VerificationFailed(
            reason = JointOperationSignError.VerificationFailed.Reason.OWNER_KEY_NOT_DERIVED,
        )
        assertThat(actual).isEqualTo(expected.left())
        coVerify(exactly = 0) { verifier.verify(any(), any(), any(), any(), any()) }
        coVerify(exactly = 1) { ownerAddressResolver.resolve(any(), any()) }
    }

    @Test
    fun `GIVEN owner address resolver fails WHEN invoke THEN Failed with the cause instead of a key error`() =
        runTest {
            // Arrange
            val cause = IllegalStateException("derived keys storage unavailable")
            coEvery { ownerAddressResolver.resolve(any(), any()) } returns cause.left()

            // Act
            val actual = useCase(account = account, network = network, operationId = OPERATION_ID, displayed = displayed)

            // Assert
            assertThat(actual).isEqualTo(JointOperationSignError.Failed(cause = cause).left())
            coVerify(exactly = 0) { signer.signOperation(any(), any(), any(), any(), any()) }
        }

    @Test
    fun `GIVEN no in-flight operation WHEN invoke THEN OperationNotFound`() = runTest {
        // Arrange
        coEvery { supplier.invoke(userWalletId = MockAccounts.userWalletId) } returns
            flowOf(listOf(jointAccount(inFlightOperations = emptyMap())))

        // Act
        val actual = useCase(account = account, network = network, operationId = OPERATION_ID, displayed = displayed)

        // Assert
        assertThat(actual).isEqualTo(JointOperationSignError.OperationNotFound.left())
        coVerify(exactly = 0) { signer.signOperation(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN operation status unknown WHEN invoke THEN Failed and nothing is signed`() = runTest {
        // Arrange
        coEvery { supplier.invoke(userWalletId = MockAccounts.userWalletId) } returns
            flowOf(
                listOf(
                    jointAccount(
                        inFlightOperations = mapOf(
                            CHAIN_ID to operation(status = JointOperation.Status.UNKNOWN),
                        ),
                    ),
                ),
            )

        // Act
        val actual = useCase(account = account, network = network, operationId = OPERATION_ID, displayed = displayed)

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(JointOperationSignError.Failed::class.java)
        coVerify(exactly = 0) { signer.signOperation(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN operation already pending WHEN invoke THEN ThresholdReached`() = runTest {
        // Arrange
        coEvery { supplier.invoke(userWalletId = MockAccounts.userWalletId) } returns
            flowOf(
                listOf(
                    jointAccount(
                        inFlightOperations = mapOf(
                            CHAIN_ID to operation(status = JointOperation.Status.PENDING),
                        ),
                    ),
                ),
            )

        // Act
        val actual = useCase(account = account, network = network, operationId = OPERATION_ID, displayed = displayed)

        // Assert
        assertThat(actual).isEqualTo(JointOperationSignError.ThresholdReached.left())
        coVerify(exactly = 0) { signer.signOperation(any(), any(), any(), any(), any()) }
    }

    private fun jointAccount(
        inFlightOperations: Map<Long, JointOperation> = mapOf(CHAIN_ID to operation()),
        safeAddress: String? = SAFE_ADDRESS,
    ): JointAccount = JointAccount(
        cryptoAccountId = account.accountId.value,
        membersCount = 2,
        threshold = 2,
        safeAddress = safeAddress,
        status = JointAccount.Status.ACTIVE,
        members = listOf(
            JointAccount.Member(name = "Alice", address = ALICE, role = JointAccount.Role.CREATOR),
        ),
        source = StatusSource.ACTUAL,
        inFlightOperations = inFlightOperations,
    )

    private fun operation(
        status: JointOperation.Status = JointOperation.Status.AWAITING_SIGNATURES,
    ): JointOperation = JointOperation(
        operationId = OPERATION_ID,
        chainId = CHAIN_ID,
        safeTx = SafeTx(
            to = RECIPIENT,
            value = AMOUNT,
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
        const val ALICE = "0x7e5f4552091a69125d5dfcb7b8c2659029395bdf"
        const val RECIPIENT = "0x1111111111111111111111111111111111111111"
        const val SAFE_ADDRESS = "0x5afe000000000000000000000000000000000001"
        const val SIGNATURE = "0xdeadbeef"
        const val OPERATION_ID = "3f1a2b4c-5d6e-7f80-91a2-b3c4d5e6f708"
        const val CHAIN_ID = 137L
        const val REFUND_RECEIVER = "0xfee0000000000000000000000000000000000001"
        val AMOUNT: BigInteger = BigInteger("1000000000000000000")
        val GAS_PRICE: BigInteger = BigInteger("30000000000")
    }
}