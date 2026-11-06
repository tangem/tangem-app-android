package com.tangem.data.jointaccount.usecase

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.data.jointaccount.safe.DefaultJointTransferPayloadCodec
import com.tangem.domain.jointaccount.address.JointOwnerAddressResolver
import com.tangem.domain.jointaccount.blockaid.JointOperationBlockaidGate
import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.domain.jointaccount.gas.SafeTxGasBuilder
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.model.JointOperationCreateError
import com.tangem.domain.jointaccount.model.JointOperationSigningError
import com.tangem.domain.jointaccount.model.JointTransferIntent
import com.tangem.domain.jointaccount.model.PreparedJointOperation
import com.tangem.domain.jointaccount.model.SafeGasToken
import com.tangem.domain.jointaccount.model.SafeInnerCall
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.jointaccount.network.JointNetworkReader
import com.tangem.domain.jointaccount.repository.JointAccountRepository
import com.tangem.domain.jointaccount.safe.SafeCompositionChecker
import com.tangem.domain.jointaccount.safe.SafeDeployment
import com.tangem.domain.jointaccount.signing.JointOperationSigner
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.jointaccount.usecase.CreateJointOperationUseCase
import com.tangem.domain.jointaccount.usecase.CreateJointOperationUseCase.SubmitResult
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.network.Network
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.MockAccounts
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.slot
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CreateJointOperationUseCaseTest {

    private val repository: JointAccountRepository = mockk()
    private val signer: JointOperationSigner = mockk()
    private val fetcher: SingleJointAccountListFetcher = mockk()
    private val supplier: SingleJointAccountListSupplier = mockk()
    private val gasBuilder: SafeTxGasBuilder = mockk()
    private val compositionChecker: SafeCompositionChecker = mockk()
    private val networkReader: JointNetworkReader = mockk()
    private val blockaidGate: JointOperationBlockaidGate = mockk()
    private val ownerAddressResolver: JointOwnerAddressResolver = mockk()
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
        blockaidGate = blockaidGate,
        ownerAddressResolver = ownerAddressResolver,
    )

    private val account = MockAccounts.createJointAccount(ownerKeyIndex = 3)
    private val intent = JointTransferIntent.Native(recipient = RECIPIENT, amountWei = AMOUNT)

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            repository,
            signer,
            fetcher,
            supplier,
            gasBuilder,
            compositionChecker,
            networkReader,
            blockaidGate,
            ownerAddressResolver,
        )
        coEvery { ownerAddressResolver.resolve(MockAccounts.userWalletId, account.ownerKeyIndex) } returns
            ALICE.right()
        coEvery { blockaidGate.verify(any(), any(), any(), any(), any(), any()) } returns
            JointOperationBlockaidGate.Verdict.Safe
        coEvery { fetcher.invoke(params = any()) } returns Unit.right()
        coEvery { supplier.invoke(userWalletId = MockAccounts.userWalletId) } returns
            flowOf(listOf(jointAccount()))
        coEvery { networkReader.getChainId(network) } returns CHAIN_ID.right()
        coEvery { compositionChecker.ensureMatches(any(), any(), any(), any(), any()) } returns
            SafeDeployment.DEPLOYED.right()
        coEvery { gasBuilder.build(any(), any(), call = any(), gasToken = SafeGasToken.Coin) } returns safeTx().right()
        coEvery { signer.signOperation(any(), any(), any(), any(), any()) } returns SIGNATURE.right()
        coEvery { repository.createOperation(any(), any(), any(), any(), any()) } returns Unit.right()
    }

    @Test
    fun `GIVEN active account WHEN create THEN gate check build sign create fetch in order`() = runTest {
        // Arrange
        val callSlot = slot<SafeInnerCall>()
        coEvery { gasBuilder.build(any(), any(), call = capture(callSlot), gasToken = SafeGasToken.Coin) } returns
            safeTx().right()

        // Act
        val actual = create(account = account, network = network, intent = intent, gasToken = SafeGasToken.Coin)

        // Assert
        assertThat(actual).isEqualTo(SubmitResult.Created.right())
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
            gasBuilder.build(
                userWalletId = MockAccounts.userWalletId,
                network = network,
                call = any(),
                gasToken = SafeGasToken.Coin,
            )
            blockaidGate.verify(
                network = network,
                safeTx = safeTx(),
                chainId = CHAIN_ID,
                safeAddress = SAFE_ADDRESS,
                ownerAddress = ALICE,
                deployment = SafeDeployment.DEPLOYED,
            )
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
    fun `GIVEN erc20 intent WHEN create THEN the inner call carries transfer calldata and zero value`() = runTest {
        // Arrange
        val callSlot = slot<SafeInnerCall>()
        coEvery { gasBuilder.build(any(), any(), call = capture(callSlot), gasToken = SafeGasToken.Coin) } returns
            safeTx().right()

        // Act
        val actual = create(
            account = account,
            network = network,
            intent = JointTransferIntent.Erc20(tokenContract = TOKEN, recipient = RECIPIENT, amountWei = AMOUNT),
            gasToken = SafeGasToken.Coin,
        )

        // Assert
        assertThat(actual).isEqualTo(SubmitResult.Created.right())
        val captured = callSlot.captured
        assertThat(captured.to).isEqualTo(TOKEN.lowercase())
        assertThat(captured.value).isEqualTo(BigInteger.ZERO)
        assertThat(captured.data).startsWith("0xa9059cbb")
    }

    @Test
    fun `GIVEN slot busy WHEN create THEN SlotBusy and nothing is built or signed`() = runTest {
        // Arrange
        coEvery { supplier.invoke(userWalletId = MockAccounts.userWalletId) } returns
            flowOf(listOf(jointAccount(inFlightOperations = mapOf(CHAIN_ID to jointOperation()))))

        // Act
        val actual = create(account = account, network = network, intent = intent, gasToken = SafeGasToken.Coin)

        // Assert
        assertThat(actual).isEqualTo(JointOperationCreateError.SlotBusy.left())
        coVerify(exactly = 0) { gasBuilder.build(any(), any(), any(), any()) }
        coVerify(exactly = 0) { signer.signOperation(any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { repository.createOperation(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN account not active WHEN create THEN AccountNotReady`() = runTest {
        // Arrange
        coEvery { supplier.invoke(userWalletId = MockAccounts.userWalletId) } returns
            flowOf(listOf(jointAccount(status = JointAccount.Status.CONFIRMING)))

        // Act
        val actual = create(account = account, network = network, intent = intent, gasToken = SafeGasToken.Coin)

        // Assert
        assertThat(actual).isEqualTo(JointOperationCreateError.AccountNotReady.left())
        coVerify(exactly = 0) { signer.signOperation(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN composition mismatch WHEN create THEN blocked before anything is signed`() = runTest {
        // Arrange
        coEvery { compositionChecker.ensureMatches(any(), any(), any(), any(), any()) } returns
            JointOperationCreateError.CompositionMismatch.left()

        // Act
        val actual = create(account = account, network = network, intent = intent, gasToken = SafeGasToken.Coin)

        // Assert
        assertThat(actual).isEqualTo(JointOperationCreateError.CompositionMismatch.left())
        coVerify(exactly = 0) { signer.signOperation(any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { repository.createOperation(any(), any(), any(), any(), any()) }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a blockaid verdict WHEN prepare THEN it is handed back with the checked payload and nothing is signed`(
        verdict: JointOperationBlockaidGate.Verdict,
    ) = runTest {
        // Arrange
        coEvery { blockaidGate.verify(any(), any(), any(), any(), any(), any()) } returns verdict

        // Act
        val actual = useCase.prepare(account, network, intent, SafeGasToken.Coin)

        // Assert
        assertThat(actual).isEqualTo(prepared(verdict = verdict).right())
        coVerify(exactly = 0) { signer.signOperation(any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { repository.createOperation(any(), any(), any(), any(), any()) }
    }

    private fun provideTestModels() = listOf(
        JointOperationBlockaidGate.Verdict.Safe,
        JointOperationBlockaidGate.Verdict.Warning,
        JointOperationBlockaidGate.Verdict.Malicious,
        JointOperationBlockaidGate.Verdict.NoVerdict(JointOperationBlockaidGate.Verdict.NoVerdict.Reason.ERROR),
        JointOperationBlockaidGate.Verdict.NoVerdict(JointOperationBlockaidGate.Verdict.NoVerdict.Reason.TIMEOUT),
        JointOperationBlockaidGate.Verdict.NoVerdict(
            JointOperationBlockaidGate.Verdict.NoVerdict.Reason.UNSUPPORTED_NETWORK,
        ),
    )

    @Test
    fun `GIVEN a prepared operation WHEN submit THEN exactly its payload is signed without another check`() = runTest {
        // Arrange
        val prepared = prepared(verdict = JointOperationBlockaidGate.Verdict.Warning)
            .copy(safeTx = safeTx().copy(nonce = BigInteger.TEN))

        // Act
        val actual = useCase.submit(prepared = prepared)

        // Assert
        assertThat(actual).isEqualTo(SubmitResult.Created.right())
        coVerify(exactly = 1) {
            signer.signOperation(any(), any(), safeTx = safeTx().copy(nonce = BigInteger.TEN), any(), any())
        }
        coVerify(exactly = 0) { blockaidGate.verify(any(), any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { gasBuilder.build(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN a malicious prepared operation WHEN submit THEN Malicious and nothing is signed`() = runTest {
        // Act
        val actual = useCase.submit(prepared = prepared(verdict = JointOperationBlockaidGate.Verdict.Malicious))

        // Assert
        assertThat(actual).isEqualTo(JointOperationCreateError.Malicious.left())
        coVerify(exactly = 0) { signer.signOperation(any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { repository.createOperation(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN network moved WHEN submit THEN the rebuilt and re-checked operation comes back unsigned`() = runTest {
        // Arrange
        coEvery { repository.createOperation(any(), any(), any(), any(), any()) } returns
            JointOperationCreateError.NetworkStateMoved.left()
        coEvery { blockaidGate.verify(any(), any(), any(), any(), any(), any()) } returns
            JointOperationBlockaidGate.Verdict.Malicious

        // Act
        val actual = useCase.submit(prepared = prepared(verdict = JointOperationBlockaidGate.Verdict.Safe))

        // Assert
        val expected = prepared(verdict = JointOperationBlockaidGate.Verdict.Malicious).copy(isRebuilt = true)
        assertThat(actual).isEqualTo(SubmitResult.Rebuilt(prepared = expected).right())
        coVerify(exactly = 1) { signer.signOperation(any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { fetcher.invoke(params = any<SingleJointAccountListFetcher.Params>()) }
    }

    @Test
    fun `GIVEN a rebuilt operation WHEN submit hits a moved network again THEN NetworkStateMoved without a rebuild`() =
        runTest {
            // Arrange
            coEvery { repository.createOperation(any(), any(), any(), any(), any()) } returns
                JointOperationCreateError.NetworkStateMoved.left()
            val rebuilt = prepared(verdict = JointOperationBlockaidGate.Verdict.Warning).copy(isRebuilt = true)

            // Act
            val actual = useCase.submit(prepared = rebuilt)

            // Assert
            assertThat(actual).isEqualTo(JointOperationCreateError.NetworkStateMoved.left())
            coVerify(exactly = 1) { signer.signOperation(any(), any(), any(), any(), any()) }
            coVerify(exactly = 0) { gasBuilder.build(any(), any(), any(), any()) }
            coVerify(exactly = 0) { blockaidGate.verify(any(), any(), any(), any(), any(), any()) }
        }

    @Test
    fun `GIVEN a safe not deployed yet WHEN prepare THEN blockaid checks the first operation as such`() = runTest {
        // Arrange
        coEvery { compositionChecker.ensureMatches(any(), any(), any(), any(), any()) } returns
            SafeDeployment.NOT_DEPLOYED.right()

        // Act
        create(account = account, network = network, intent = intent, gasToken = SafeGasToken.Coin)

        // Assert
        coVerify(exactly = 1) {
            blockaidGate.verify(any(), any(), any(), any(), any(), deployment = SafeDeployment.NOT_DEPLOYED)
        }
    }

    @Test
    fun `GIVEN owner key not derived WHEN create THEN nothing is checked or signed`() = runTest {
        // Arrange
        coEvery { ownerAddressResolver.resolve(MockAccounts.userWalletId, account.ownerKeyIndex) } returns
            null.right()

        // Act
        val actual = create(account = account, network = network, intent = intent, gasToken = SafeGasToken.Coin)

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(JointOperationCreateError.Failed::class.java)
        coVerify(exactly = 0) { blockaidGate.verify(any(), any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { signer.signOperation(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN recipient is the safe itself WHEN create THEN InvalidPayload and nothing signed`() = runTest {
        // Act
        val actual = create(
            account = account,
            network = network,
            intent = JointTransferIntent.Native(recipient = SAFE_ADDRESS.uppercase().replace("0X", "0x"), amountWei = AMOUNT),
            gasToken = SafeGasToken.Coin,
        )

        // Assert
        assertThat(actual).isEqualTo(JointOperationCreateError.InvalidPayload.left())
        coVerify(exactly = 0) { signer.signOperation(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN amount beyond uint256 WHEN create THEN InvalidPayload instead of a crash`() = runTest {
        // Act
        val actual = create(
            account = account,
            network = network,
            intent = JointTransferIntent.Erc20(
                tokenContract = TOKEN,
                recipient = RECIPIENT,
                amountWei = BigInteger.ONE.shiftLeft(256),
            ),
            gasToken = SafeGasToken.Coin,
        )

        // Assert
        assertThat(actual).isEqualTo(JointOperationCreateError.InvalidPayload.left())
        coVerify(exactly = 0) { signer.signOperation(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN zero amount WHEN create THEN InvalidPayload`() = runTest {
        // Act
        val actual = create(
            account = account,
            network = network,
            intent = JointTransferIntent.Native(recipient = RECIPIENT, amountWei = BigInteger.ZERO),
            gasToken = SafeGasToken.Coin,
        )

        // Assert
        assertThat(actual).isEqualTo(JointOperationCreateError.InvalidPayload.left())
        coVerify(exactly = 0) { signer.signOperation(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN network moved under first attempt WHEN create THEN rebuilt and honestly re-signed`() = runTest {
        // Arrange
        coEvery { repository.createOperation(any(), any(), any(), any(), any()) } returnsMany listOf(
            JointOperationCreateError.NetworkStateMoved.left(),
            Unit.right(),
        )

        // Act
        val actual = create(account = account, network = network, intent = intent, gasToken = SafeGasToken.Coin)

        // Assert
        assertThat(actual).isEqualTo(SubmitResult.Created.right())
        // Every signature request is preceded by its own composition check
        coVerify(exactly = 2) { compositionChecker.ensureMatches(any(), any(), any(), any(), any()) }
        coVerify(exactly = 2) { gasBuilder.build(any(), any(), any(), any()) }
        coVerify(exactly = 2) { signer.signOperation(any(), any(), any(), any(), any()) }
        coVerify(exactly = 2) { repository.createOperation(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN a token gas and the network moved WHEN create THEN both attempts are priced in that token`() = runTest {
        // Arrange
        val gasToken = SafeGasToken.Token(
            address = TOKEN.lowercase(),
            decimals = 6,
            coinDecimals = 18,
            nativeToTokenRate = BigDecimal("0.2"),
        )
        coEvery { gasBuilder.build(any(), any(), call = any(), gasToken = gasToken) } returns safeTx().right()
        coEvery { repository.createOperation(any(), any(), any(), any(), any()) } returnsMany listOf(
            JointOperationCreateError.NetworkStateMoved.left(),
            Unit.right(),
        )

        // Act
        val actual = create(account = account, network = network, intent = intent, gasToken = gasToken)

        // Assert
        assertThat(actual).isEqualTo(SubmitResult.Created.right())
        coVerify(exactly = 2) { gasBuilder.build(any(), any(), any(), gasToken = gasToken) }
        coVerify(exactly = 0) { gasBuilder.build(any(), any(), any(), gasToken = SafeGasToken.Coin) }
    }

    @Test
    fun `GIVEN network moved twice WHEN create THEN NetworkStateMoved without a third tap`() = runTest {
        // Arrange
        coEvery { repository.createOperation(any(), any(), any(), any(), any()) } returns
            JointOperationCreateError.NetworkStateMoved.left()

        // Act
        val actual = create(account = account, network = network, intent = intent, gasToken = SafeGasToken.Coin)

        // Assert
        assertThat(actual).isEqualTo(JointOperationCreateError.NetworkStateMoved.left())
        coVerify(exactly = 2) { signer.signOperation(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN user cancels the tap WHEN create THEN UserCancelled and nothing is sent`() = runTest {
        // Arrange
        coEvery { signer.signOperation(any(), any(), any(), any(), any()) } returns
            JointOperationSigningError.UserCancelled.left()

        // Act
        val actual = create(account = account, network = network, intent = intent, gasToken = SafeGasToken.Coin)

        // Assert
        assertThat(actual).isEqualTo(JointOperationCreateError.UserCancelled.left())
        coVerify(exactly = 0) { repository.createOperation(any(), any(), any(), any(), any()) }
    }

    private suspend fun create(
        account: Account.Joint,
        network: Network,
        intent: JointTransferIntent,
        gasToken: SafeGasToken,
    ): Either<JointOperationCreateError, SubmitResult> = useCase
        .prepare(account = account, network = network, intent = intent, gasToken = gasToken)
        .flatMap { prepared -> useCase.submit(prepared = prepared) }
        .flatMap { result ->
            when (result) {
                SubmitResult.Created -> result.right()
                is SubmitResult.Rebuilt -> useCase.submit(prepared = result.prepared)
            }
        }

    private fun prepared(verdict: JointOperationBlockaidGate.Verdict) = PreparedJointOperation.Creation(
        account = account,
        network = network,
        safeTx = safeTx(),
        verdict = verdict,
        jointAccount = jointAccount(),
        call = SafeInnerCall(to = RECIPIENT.lowercase(), value = AMOUNT, data = "0x"),
        gasToken = SafeGasToken.Coin,
        chainId = CHAIN_ID,
        safeAddress = SAFE_ADDRESS,
        ownerAddress = ALICE,
    )

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