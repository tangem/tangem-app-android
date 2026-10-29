package com.tangem.features.txhistory.joint.operation.model

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.addressbook.usecase.IsAddressBookCompatibleUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.jointaccount.address.JointOwnerAddressResolver
import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.domain.jointaccount.model.FinishedJointOperation
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.model.JointOperationSignerState
import com.tangem.domain.jointaccount.model.JointOperationTransfer
import com.tangem.domain.jointaccount.model.JointTransferIntent
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.jointaccount.network.JointNetworkReader
import com.tangem.domain.jointaccount.safe.JointOperationSignersResolver
import com.tangem.domain.jointaccount.safe.JointOperationTransferResolver
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.jointaccount.usecase.RejectJointOperationUseCase
import com.tangem.domain.jointaccount.usecase.SignJointOperationUseCase
import com.tangem.domain.jointaccount.verifier.JointOperationDisplayGate
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.model.JointOperationTx
import com.tangem.domain.txhistory.repository.JointOperationHistoryRepository
import com.tangem.features.txhistory.joint.operation.signers.FinishedJointOperationSigners
import com.tangem.features.txhistory.joint.operation.signers.ui.state.JointOperationSignersUM
import com.tangem.features.txhistory.model.TxHistoryOwnerLookupProducer
import com.tangem.test.core.getEmittedValues
import com.tangem.test.mock.MockAccounts
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigInteger

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JointOperationDelegateTest {

    private val router: Router = mockk()
    private val accountListSupplier: SingleAccountListSupplier = mockk()
    private val jointAccountListSupplier: SingleJointAccountListSupplier = mockk()
    private val jointAccountListFetcher: SingleJointAccountListFetcher = mockk()
    private val historyRepository: JointOperationHistoryRepository = mockk()
    private val finishedSigners: FinishedJointOperationSigners = mockk()
    private val transferResolver: JointOperationTransferResolver = mockk()
    private val signersResolver: JointOperationSignersResolver = mockk()
    private val ownerAddressResolver: JointOwnerAddressResolver = mockk()
    private val networkReader: JointNetworkReader = mockk()
    private val signJointOperationUseCase: SignJointOperationUseCase = mockk()
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase = mockk()
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase = mockk()
    private val rejectJointOperationUseCase: RejectJointOperationUseCase = mockk()
    private val isAddressBookCompatibleUseCase: IsAddressBookCompatibleUseCase = mockk()
    private val ownerLookupProducer: TxHistoryOwnerLookupProducer = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk()
    private val displayGate: JointOperationDisplayGate = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            router,
            accountListSupplier,
            jointAccountListSupplier,
            jointAccountListFetcher,
            historyRepository,
            finishedSigners,
            transferResolver,
            signersResolver,
            ownerAddressResolver,
            networkReader,
            signJointOperationUseCase,
            getWalletMetaInfoUseCase,
            sendFeedbackEmailUseCase,
            rejectJointOperationUseCase,
            isAddressBookCompatibleUseCase,
            ownerLookupProducer,
            userWalletsListRepository,
            singleAccountStatusListSupplier,
            getSelectedAppCurrencyUseCase,
            displayGate,
        )

        every { accountListSupplier(USER_WALLET_ID) } returns flowOf(accountListWithJointAccount)
        coEvery { displayGate.isShown(any(), any(), any(), any(), any()) } returns true
        every {
            transferResolver.resolve(safeTx = any(), currencies = any(), network = any(), safeAddress = any())
        } returns JointOperationTransfer(currency = jointCurrency, intent = TRANSFER_INTENT)
        every { signersResolver.signers(jointAccount = any(), operation = any<JointOperation>()) } returns SIGNERS
        every {
            signersResolver.canSign(jointAccount = any(), operation = any(), ownerAddress = any())
        } returns true
        coEvery {
            ownerAddressResolver.resolve(userWalletId = any(), ownerKeyIndex = any())
        } returns OWNER_ADDRESS.right()
        coEvery { networkReader.getChainId(network = jointCurrency.network) } returns CHAIN_ID.right()
        every { jointAccountListSupplier(USER_WALLET_ID) } returns flowOf(listOf(createJointAccount()))
        coEvery { jointAccountListFetcher(userWalletId = any()) } returns Unit.right()
        every { historyRepository.observeOperation(USER_WALLET_ID, OPERATION_ID) } returns flowOf(null)
        every { finishedSigners.signatures(jointAccount = any(), operation = any()) } returns SIGNATURES
    }

    @Test
    fun `GIVEN the operation in flight in the list WHEN observe THEN its state resolves`() = runTest {
        // Act
        val values = getEmittedValues(flow = createDelegate().observe(USER_WALLET_ID, jointCurrency, OPERATION_ID))
        advanceUntilIdle()

        // Assert
        assertThat(values).containsExactly(JointOperationSnapshot.InFlight(expectedState))
        coVerify(exactly = 0) { jointAccountListFetcher(userWalletId = any()) }
    }

    @Test
    fun `GIVEN the operation missing from the list WHEN observe THEN the list is fetched once`() = runTest {
        // Arrange
        val accounts = MutableStateFlow(listOf(createJointAccount(inFlightOperations = emptyMap())))
        every { jointAccountListSupplier(USER_WALLET_ID) } returns accounts
        getEmittedValues(flow = createDelegate().observe(USER_WALLET_ID, jointCurrency, OPERATION_ID))
        advanceUntilIdle()

        // Act
        accounts.value = listOf(createJointAccount(inFlightOperations = emptyMap(), threshold = THRESHOLD + 1))
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { jointAccountListFetcher(userWalletId = USER_WALLET_ID) }
    }

    @Test
    fun `GIVEN the operation neither in flight nor finished WHEN observe THEN it is not synced yet`() = runTest {
        // Arrange
        every { jointAccountListSupplier(USER_WALLET_ID) } returns
            flowOf(listOf(createJointAccount(inFlightOperations = emptyMap())))

        // Act
        val values = getEmittedValues(flow = createDelegate().observe(USER_WALLET_ID, jointCurrency, OPERATION_ID))
        advanceUntilIdle()

        // Assert
        assertThat(values).containsExactly(JointOperationSnapshot.NotSynced)
    }

    @Test
    fun `GIVEN the joint account list is not loaded yet WHEN observe THEN it is not synced yet`() = runTest {
        // Arrange
        every { jointAccountListSupplier(USER_WALLET_ID) } returns flowOf(emptyList())

        // Act
        val values = getEmittedValues(flow = createDelegate().observe(USER_WALLET_ID, jointCurrency, OPERATION_ID))
        advanceUntilIdle()

        // Assert
        assertThat(values).containsExactly(JointOperationSnapshot.NotSynced)
    }

    @Test
    fun `GIVEN a status this build does not know WHEN observe THEN it is unreadable`() = runTest {
        // Arrange
        every { jointAccountListSupplier(USER_WALLET_ID) } returns
            flowOf(listOf(createJointAccount(operation = createOperation(status = JointOperation.Status.UNKNOWN))))

        // Act
        val values = getEmittedValues(flow = createDelegate().observe(USER_WALLET_ID, jointCurrency, OPERATION_ID))
        advanceUntilIdle()

        // Assert
        assertThat(values).containsExactly(JointOperationSnapshot.Unreadable)
    }

    @Test
    fun `GIVEN a finished operation still in the list WHEN observe THEN the outcome wins`() = runTest {
        // Arrange
        every { historyRepository.observeOperation(USER_WALLET_ID, OPERATION_ID) } returns
            flowOf(finishedOperation)

        // Act
        val values = getEmittedValues(flow = createDelegate().observe(USER_WALLET_ID, jointCurrency, OPERATION_ID))
        advanceUntilIdle()

        // Assert
        assertThat(values).containsExactly(
            JointOperationSnapshot.Finished(
                tx = JointOperationTx(
                    operation = finishedOperation,
                    transfer = JointOperationTransfer(currency = jointCurrency, intent = TRANSFER_INTENT),
                    txInfo = null,
                ),
                signatures = SIGNATURES,
            ),
        )
    }

    @Test
    fun `GIVEN a finished transfer this build cannot read WHEN observe THEN it is unreadable`() = runTest {
        // Arrange
        every { historyRepository.observeOperation(USER_WALLET_ID, OPERATION_ID) } returns
            flowOf(finishedOperation)
        every {
            transferResolver.resolve(safeTx = any(), currencies = any(), network = any(), safeAddress = any())
        } returns null

        // Act
        val values = getEmittedValues(flow = createDelegate().observe(USER_WALLET_ID, jointCurrency, OPERATION_ID))
        advanceUntilIdle()

        // Assert
        assertThat(values).containsExactly(JointOperationSnapshot.Unreadable)
    }

    @Test
    fun `GIVEN the operation shown in flight WHEN it stops passing the display gate THEN it is unreadable`() =
        runTest {
            // Arrange
            val accounts = MutableStateFlow(listOf(createJointAccount()))
            every { jointAccountListSupplier(USER_WALLET_ID) } returns accounts
            val values = getEmittedValues(
                flow = createDelegate().observe(USER_WALLET_ID, jointCurrency, OPERATION_ID),
            )
            advanceUntilIdle()

            // Act
            coEvery { displayGate.isShown(any(), any(), any(), any(), any()) } returns false
            accounts.value = listOf(createJointAccount(threshold = THRESHOLD + 1))
            advanceUntilIdle()

            // Assert
            assertThat(values.last()).isEqualTo(JointOperationSnapshot.Unreadable)
        }

    @Test
    fun `GIVEN the operation does not pass the display gate WHEN observe THEN it is unreadable`() = runTest {
        // Arrange
        every { jointAccountListSupplier(USER_WALLET_ID) } returns flowOf(listOf(createJointAccount()))
        coEvery { displayGate.isShown(any(), any(), any(), any(), any()) } returns false

        // Act
        val values = getEmittedValues(flow = createDelegate().observe(USER_WALLET_ID, jointCurrency, OPERATION_ID))
        advanceUntilIdle()

        // Assert
        assertThat(values).containsExactly(JointOperationSnapshot.Unreadable)
        coVerify(exactly = 0) { signersResolver.signers(jointAccount = any(), operation = any<JointOperation>()) }
    }

    @Test
    fun `GIVEN a token refund WHEN observe THEN the fee is in the account's token on that network`() = runTest {
        // Arrange
        val account = MockAccounts.createJointAccount(
            cryptoCurrencies = listOf(jointCurrency, jointUsdc),
            userWalletId = USER_WALLET_ID,
        )
        every { accountListSupplier(USER_WALLET_ID) } returns flowOf(accountListWith(account))
        every { jointAccountListSupplier(USER_WALLET_ID) } returns flowOf(
            listOf(
                createJointAccount(
                    cryptoAccountId = account.accountId.value,
                    operation = createOperation(safeTx = createSafeTx(gasToken = USDC_ADDRESS.uppercase())),
                ),
            ),
        )

        // Act
        val values = getEmittedValues(flow = createDelegate().observe(USER_WALLET_ID, jointCurrency, OPERATION_ID))
        advanceUntilIdle()

        // Assert
        assertThat(values).containsExactly(
            JointOperationSnapshot.InFlight(expectedState.copy(account = account, feeCurrency = jointUsdc)),
        )
    }

    @Test
    fun `GIVEN a gas token outside the account WHEN observe THEN the state has no fee currency`() = runTest {
        // Arrange
        every { jointAccountListSupplier(USER_WALLET_ID) } returns flowOf(
            listOf(createJointAccount(operation = createOperation(safeTx = createSafeTx(gasToken = USDC_ADDRESS)))),
        )

        // Act
        val values = getEmittedValues(flow = createDelegate().observe(USER_WALLET_ID, jointCurrency, OPERATION_ID))
        advanceUntilIdle()

        // Assert
        assertThat(values).containsExactly(JointOperationSnapshot.InFlight(expectedState.copy(feeCurrency = null)))
    }

    @Test
    fun `GIVEN a finished operation of another Safe WHEN observe THEN it is unreadable`() = runTest {
        // Arrange
        every { historyRepository.observeOperation(USER_WALLET_ID, OPERATION_ID) } returns
            flowOf(finishedOperation.copy(safeAddress = "0xanothersafe"))

        // Act
        val values = getEmittedValues(flow = createDelegate().observe(USER_WALLET_ID, jointCurrency, OPERATION_ID))
        advanceUntilIdle()

        // Assert
        assertThat(values).containsExactly(JointOperationSnapshot.Unreadable)
    }

    @Test
    fun `GIVEN a finished operation in another network WHEN observe THEN it is unreadable`() = runTest {
        // Arrange
        every { historyRepository.observeOperation(USER_WALLET_ID, OPERATION_ID) } returns
            flowOf(finishedOperation.copy(chainId = CHAIN_ID + 1))

        // Act
        val values = getEmittedValues(flow = createDelegate().observe(USER_WALLET_ID, jointCurrency, OPERATION_ID))
        advanceUntilIdle()

        // Assert
        assertThat(values).containsExactly(JointOperationSnapshot.Unreadable)
    }

    @Test
    fun `GIVEN another operation in the slot WHEN observe THEN it is not synced yet`() = runTest {
        // Arrange
        every { jointAccountListSupplier(USER_WALLET_ID) } returns
            flowOf(listOf(createJointAccount(operation = createOperation(operationId = "operation-2"))))

        // Act
        val values = getEmittedValues(flow = createDelegate().observe(USER_WALLET_ID, jointCurrency, OPERATION_ID))
        advanceUntilIdle()

        // Assert
        assertThat(values).containsExactly(JointOperationSnapshot.NotSynced)
    }

    @Test
    fun `GIVEN the operation leaves the list WHEN its outcome is not stored yet THEN the last state is held`() =
        runTest {
            // Arrange
            val accounts = MutableStateFlow(listOf(createJointAccount()))
            every { jointAccountListSupplier(USER_WALLET_ID) } returns accounts
            val values = getEmittedValues(
                flow = createDelegate().observe(USER_WALLET_ID, jointCurrency, OPERATION_ID),
            )
            advanceUntilIdle()

            // Act
            accounts.value = listOf(createJointAccount(inFlightOperations = emptyMap()))
            advanceUntilIdle()

            // Assert
            assertThat(values).containsExactly(
                JointOperationSnapshot.InFlight(expectedState),
                JointOperationSnapshot.InFlight(expectedState, isHeld = true),
            ).inOrder()
        }

    @Test
    fun `GIVEN the operation in flight WHEN its outcome is stored THEN it is finished`() = runTest {
        // Arrange
        val finished = MutableStateFlow<FinishedJointOperation?>(null)
        every { historyRepository.observeOperation(USER_WALLET_ID, OPERATION_ID) } returns finished
        val values = getEmittedValues(flow = createDelegate().observe(USER_WALLET_ID, jointCurrency, OPERATION_ID))
        advanceUntilIdle()

        // Act
        finished.value = finishedOperation
        advanceUntilIdle()

        // Assert
        assertThat(values.last()).isInstanceOf(JointOperationSnapshot.Finished::class.java)
    }

    @Test
    fun `GIVEN the wallet has no joint account for the currency WHEN observe THEN it is unreadable`() = runTest {
        // Arrange
        every { accountListSupplier(USER_WALLET_ID) } returns flowOf(
            MockAccounts.createAccountList(activeAccounts = 1, userWalletId = USER_WALLET_ID),
        )

        // Act
        val values = getEmittedValues(flow = createDelegate().observe(USER_WALLET_ID, jointCurrency, OPERATION_ID))
        advanceUntilIdle()

        // Assert
        assertThat(values).containsExactly(JointOperationSnapshot.Unreadable)
    }

    @Test
    fun `GIVEN a currency outside a joint account WHEN observe THEN no supplier is touched`() = runTest {
        // Act
        val values = getEmittedValues(
            flow = createDelegate().observe(USER_WALLET_ID, nonJointCurrency, OPERATION_ID),
        )
        advanceUntilIdle()

        // Assert
        assertThat(values).containsExactly(JointOperationSnapshot.Unreadable)
        verify(exactly = 0) { jointAccountListSupplier(USER_WALLET_ID) }
        verify(exactly = 0) { historyRepository.observeOperation(any(), any()) }
    }

    private fun createDelegate() = JointOperationDelegate(
        router = router,
        accountListSupplier = accountListSupplier,
        jointAccountListSupplier = jointAccountListSupplier,
        jointAccountListFetcher = jointAccountListFetcher,
        historyRepository = historyRepository,
        finishedSigners = finishedSigners,
        transferResolver = transferResolver,
        signersResolver = signersResolver,
        ownerAddressResolver = ownerAddressResolver,
        networkReader = networkReader,
        signJointOperationUseCase = signJointOperationUseCase,
        getWalletMetaInfoUseCase = getWalletMetaInfoUseCase,
        sendFeedbackEmailUseCase = sendFeedbackEmailUseCase,
        rejectJointOperationUseCase = rejectJointOperationUseCase,
        isAddressBookCompatibleUseCase = isAddressBookCompatibleUseCase,
        ownerLookupProducer = ownerLookupProducer,
        userWalletsListRepository = userWalletsListRepository,
        singleAccountStatusListSupplier = singleAccountStatusListSupplier,
        getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
        displayGate = displayGate,
    )

    private fun createJointAccount(
        cryptoAccountId: String = portfolioAccount.accountId.value,
        membersCount: Int = MEMBERS_COUNT,
        threshold: Int = THRESHOLD,
        safeAddress: String? = SAFE_ADDRESS,
        status: JointAccount.Status = JointAccount.Status.ACTIVE,
        members: List<JointAccount.Member> = listOf(MEMBER),
        source: StatusSource = StatusSource.ACTUAL,
        operation: JointOperation = createOperation(),
        inFlightOperations: Map<Long, JointOperation> = mapOf(CHAIN_ID to operation),
    ) = JointAccount(
        cryptoAccountId = cryptoAccountId,
        membersCount = membersCount,
        threshold = threshold,
        safeAddress = safeAddress,
        status = status,
        members = members,
        source = source,
        inFlightOperations = inFlightOperations,
    )

    private fun createOperation(
        operationId: String = OPERATION_ID,
        chainId: Long = CHAIN_ID,
        safeTx: SafeTx = createSafeTx(),
        status: JointOperation.Status = JointOperation.Status.AWAITING_SIGNATURES,
        initiator: String? = OWNER_ADDRESS,
        signatures: List<JointOperation.MemberSignature> = emptyList(),
    ) = JointOperation(
        operationId = operationId,
        chainId = chainId,
        safeTx = safeTx,
        status = status,
        initiator = initiator,
        signatures = signatures,
    )

    private fun createSafeTx(
        to: String = RECIPIENT,
        value: BigInteger = BigInteger.TEN,
        data: String = "0x",
        operation: Int = SafeTx.OPERATION_CALL,
        safeTxGas: BigInteger = SAFE_TX_GAS,
        baseGas: BigInteger = BASE_GAS,
        gasPrice: BigInteger = GAS_PRICE,
        gasToken: String = ZERO_ADDRESS,
        refundReceiver: String = ZERO_ADDRESS,
        nonce: BigInteger = BigInteger.ZERO,
    ) = SafeTx(
        to = to,
        value = value,
        data = data,
        operation = operation,
        safeTxGas = safeTxGas,
        baseGas = baseGas,
        gasPrice = gasPrice,
        gasToken = gasToken,
        refundReceiver = refundReceiver,
        nonce = nonce,
    )

    private companion object {

        val USER_WALLET_ID: UserWalletId = MockAccounts.userWalletId
        const val OPERATION_ID = "operation-1"
        const val CHAIN_ID = 1L
        const val MEMBERS_COUNT = 2
        const val THRESHOLD = 2
        const val SAFE_ADDRESS = "0xsafe"
        const val RECIPIENT = "0xrecipient"
        const val OWNER_ADDRESS = "0xowner"
        const val ZERO_ADDRESS = "0x0000000000000000000000000000000000000000"
        const val USDC_ADDRESS = "0x3c499c542cef5e3811e1192ce70d8cc03d5c3359"

        val SAFE_TX_GAS: BigInteger = BigInteger.valueOf(21_000)
        val BASE_GAS: BigInteger = BigInteger.valueOf(30_000)
        val GAS_PRICE: BigInteger = BigInteger.valueOf(2)

        val TRANSFER_INTENT = JointTransferIntent.Native(
            recipient = RECIPIENT,
            amountWei = BigInteger.TEN,
        )

        val MEMBER = JointAccount.Member(
            name = "Owner",
            address = OWNER_ADDRESS,
            role = JointAccount.Role.CREATOR,
        )

        val SIGNERS = listOf(
            JointOperationSignerState(
                member = MEMBER,
                answer = JointOperationSignerState.Answer.CREATED,
                at = null,
            ),
        )

        val SIGNATURES = FinishedJointOperationSigners.Signatures(
            collected = 1,
            threshold = THRESHOLD,
            signers = JointOperationSignersUM(membersCount = MEMBERS_COUNT, signers = persistentListOf()),
        )

        val finishedOperation = FinishedJointOperation(
            operationId = OPERATION_ID,
            chainId = CHAIN_ID,
            safeAddress = SAFE_ADDRESS,
            safeTx = SafeTx(
                to = RECIPIENT,
                value = BigInteger.TEN,
                data = "0x",
                operation = SafeTx.OPERATION_CALL,
                safeTxGas = SAFE_TX_GAS,
                baseGas = BASE_GAS,
                gasPrice = GAS_PRICE,
                gasToken = ZERO_ADDRESS,
                refundReceiver = ZERO_ADDRESS,
                nonce = BigInteger.ZERO,
            ),
            status = FinishedJointOperation.Status.CANCELLED,
            initiator = OWNER_ADDRESS,
            signatures = emptyList(),
            createdAt = "2026-10-14T09:20:37.000Z",
            finishedAtMillis = 1_000L,
            txHash = null,
        )

        val nonJointCurrency: CryptoCurrency.Coin = MockCryptoCurrencyFactory().ethereum

        val jointCurrency: CryptoCurrency.Coin = nonJointCurrency.let { ethereum ->
            val derivationPath = Network.DerivationPath.Joint(value = "m/44'/60'/888888'/0/0")

            ethereum.copy(
                network = ethereum.network.copy(
                    id = ethereum.network.id.copy(derivationPath = derivationPath),
                    derivationPath = derivationPath,
                ),
            )
        }

        val jointUsdc: CryptoCurrency.Token = MockCryptoCurrencyFactory()
            .createToken(Blockchain.Ethereum, contractAddress = USDC_ADDRESS)
            .copy(network = jointCurrency.network)

        val portfolioAccount: Account.Joint = MockAccounts.createJointAccount(
            cryptoCurrencies = listOf(jointCurrency),
            userWalletId = USER_WALLET_ID,
        )

        val accountListWithJointAccount: AccountList = accountListWith(portfolioAccount)

        fun accountListWith(account: Account.Joint): AccountList =
            (MockAccounts.createAccountList(activeAccounts = 1, userWalletId = USER_WALLET_ID) + account).getOrNull()!!

        val expectedState = JointOperationState(
            account = portfolioAccount,
            displayed = TRANSFER_INTENT,
            feeAmount = GAS_PRICE * (SAFE_TX_GAS + BASE_GAS),
            feeCurrency = jointCurrency,
            threshold = THRESHOLD,
            membersCount = MEMBERS_COUNT,
            signers = SIGNERS,
            canSign = true,
            ownAddress = OWNER_ADDRESS,
            initiator = OWNER_ADDRESS,
            isSending = false,
        )
    }
}