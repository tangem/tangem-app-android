package com.tangem.features.txhistory.joint.operation.model

import arrow.core.left
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
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.model.JointOperationSignerState
import com.tangem.domain.jointaccount.model.JointOperationTransfer
import com.tangem.domain.jointaccount.model.JointTransferIntent
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.jointaccount.safe.JointOperationSignersResolver
import com.tangem.domain.jointaccount.safe.JointOperationTransferResolver
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.jointaccount.usecase.RejectJointOperationUseCase
import com.tangem.domain.jointaccount.usecase.SignJointOperationUseCase
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.txhistory.model.TxHistoryOwnerLookupProducer
import com.tangem.test.core.getEmittedValues
import com.tangem.test.mock.MockAccounts
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigInteger

/**

 * can be missing from this device's own cache for one fetch cycle, and the sheet must not resolve to `null`
 * (and dismiss itself) on that stale read.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JointOperationDelegateTest {

    private val router: Router = mockk()
    private val accountListSupplier: SingleAccountListSupplier = mockk()
    private val jointAccountListSupplier: SingleJointAccountListSupplier = mockk()
    private val jointAccountListFetcher: SingleJointAccountListFetcher = mockk()
    private val transferResolver: JointOperationTransferResolver = mockk()
    private val signersResolver: JointOperationSignersResolver = mockk()
    private val ownerAddressResolver: JointOwnerAddressResolver = mockk()
    private val signJointOperationUseCase: SignJointOperationUseCase = mockk()
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase = mockk()
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase = mockk()
    private val rejectJointOperationUseCase: RejectJointOperationUseCase = mockk()
    private val isAddressBookCompatibleUseCase: IsAddressBookCompatibleUseCase = mockk()
    private val ownerLookupProducer: TxHistoryOwnerLookupProducer = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            router,
            accountListSupplier,
            jointAccountListSupplier,
            jointAccountListFetcher,
            transferResolver,
            signersResolver,
            ownerAddressResolver,
            signJointOperationUseCase,
            getWalletMetaInfoUseCase,
            sendFeedbackEmailUseCase,
            rejectJointOperationUseCase,
            isAddressBookCompatibleUseCase,
            ownerLookupProducer,
            userWalletsListRepository,
            singleAccountStatusListSupplier,
            getSelectedAppCurrencyUseCase,
        )

        every { accountListSupplier(USER_WALLET_ID) } returns flowOf(accountListWithJointAccount)
        every {
            transferResolver.resolve(safeTx = any(), currencies = any(), network = any(), safeAddress = any())
        } returns JointOperationTransfer(currency = jointCurrency, intent = TRANSFER_INTENT)
        every { signersResolver.signers(jointAccount = any(), operation = any()) } returns SIGNERS
        every {
            signersResolver.canSign(jointAccount = any(), operation = any(), ownerAddress = any())
        } returns true
        coEvery {
            ownerAddressResolver.resolve(userWalletId = any(), ownerKeyIndex = any())
        } returns OWNER_ADDRESS.right()
        coEvery { jointAccountListFetcher(userWalletId = any()) } returns Unit.right()
    }

    @Test
    fun `GIVEN operation in the first emission WHEN observe THEN state resolves without a refetch`() = runTest {
        // Arrange
        every { jointAccountListSupplier(USER_WALLET_ID) } returns flowOf(listOf(createJointAccount()))

        // Act
        val values = getEmittedValues(flow = createDelegate().observe(USER_WALLET_ID, jointCurrency, OPERATION_ID))
        advanceUntilIdle()

        // Assert
        assertThat(values).containsExactly(expectedState)
        coVerify(exactly = 0) { jointAccountListFetcher(userWalletId = any()) }
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
                    inFlightOperations = mapOf(
                        CHAIN_ID to createOperation(safeTx = createSafeTx(gasToken = USDC_ADDRESS.uppercase())),
                    ),
                ),
            ),
        )

        // Act
        val values = getEmittedValues(flow = createDelegate().observe(USER_WALLET_ID, jointCurrency, OPERATION_ID))
        advanceUntilIdle()

        // Assert
        assertThat(values).containsExactly(expectedState.copy(account = account, feeCurrency = jointUsdc))
    }

    @Test
    fun `GIVEN a gas token outside the account WHEN observe THEN the state has no fee currency`() = runTest {
        // Arrange
        every { jointAccountListSupplier(USER_WALLET_ID) } returns flowOf(
            listOf(
                createJointAccount(
                    inFlightOperations = mapOf(
                        CHAIN_ID to createOperation(safeTx = createSafeTx(gasToken = USDC_ADDRESS)),
                    ),
                ),
            ),
        )

        // Act
        val values = getEmittedValues(flow = createDelegate().observe(USER_WALLET_ID, jointCurrency, OPERATION_ID))
        advanceUntilIdle()

        // Assert
        assertThat(values).containsExactly(expectedState.copy(feeCurrency = null))
    }

    @Test
    fun `GIVEN operation missing from a stale cache WHEN observe THEN one forced refetch resolves it`() = runTest {
        // Arrange
        val accounts = MutableStateFlow(listOf(createJointAccount(inFlightOperations = emptyMap())))
        every { jointAccountListSupplier(USER_WALLET_ID) } returns accounts
        coEvery { jointAccountListFetcher(userWalletId = USER_WALLET_ID) } answers {
            accounts.value = listOf(createJointAccount())
            Unit.right()
        }

        // Act
        val values = getEmittedValues(flow = createDelegate().observe(USER_WALLET_ID, jointCurrency, OPERATION_ID))
        advanceUntilIdle()

        // Assert
        assertThat(values).containsExactly(expectedState)
        coVerify(exactly = 1) { jointAccountListFetcher(userWalletId = USER_WALLET_ID) }
    }

    @Test
    fun `GIVEN operation gone even after the refetch WHEN cache emits again THEN it is refetched only once`() =
        runTest {
            // Arrange
            val accounts = MutableStateFlow(listOf(createJointAccount(inFlightOperations = emptyMap())))
            val collectedSnapshots = mutableListOf<List<JointAccount>>()
            every { jointAccountListSupplier(USER_WALLET_ID) } returns accounts.onEach(collectedSnapshots::add)

            // Act
            val values = getEmittedValues(flow = createDelegate().observe(USER_WALLET_ID, jointCurrency, OPERATION_ID))
            advanceUntilIdle()
            val secondStaleSnapshot = listOf(
                createJointAccount(inFlightOperations = emptyMap(), source = StatusSource.ONLY_CACHE),
            )
            accounts.value = secondStaleSnapshot
            advanceUntilIdle()

            // Assert
            assertThat(values).containsExactly(null)
            assertThat(collectedSnapshots).contains(secondStaleSnapshot)
            coVerify(exactly = 1) { jointAccountListFetcher(userWalletId = USER_WALLET_ID) }
        }

    @Test
    fun `GIVEN the forced refetch fails WHEN observe THEN state is null and nothing is retried`() = runTest {
        // Arrange
        val accounts = MutableStateFlow(listOf(createJointAccount(inFlightOperations = emptyMap())))
        every { jointAccountListSupplier(USER_WALLET_ID) } returns accounts
        coEvery {
            jointAccountListFetcher(userWalletId = USER_WALLET_ID)
        } returns IllegalStateException("fetch failed").left()

        // Act
        val values = getEmittedValues(flow = createDelegate().observe(USER_WALLET_ID, jointCurrency, OPERATION_ID))
        advanceUntilIdle()
        accounts.value = listOf(createJointAccount(inFlightOperations = emptyMap(), source = StatusSource.ONLY_CACHE))
        advanceUntilIdle()

        // Assert
        assertThat(values).containsExactly(null)
        coVerify(exactly = 1) { jointAccountListFetcher(userWalletId = USER_WALLET_ID) }
    }

    @Test
    fun `GIVEN the wallet has no joint account for the currency WHEN observe THEN nothing is refetched`() = runTest {
        // Arrange
        every { accountListSupplier(USER_WALLET_ID) } returns flowOf(
            MockAccounts.createAccountList(activeAccounts = 1, userWalletId = USER_WALLET_ID),
        )
        every { jointAccountListSupplier(USER_WALLET_ID) } returns flowOf(listOf(createJointAccount()))

        // Act
        val values = getEmittedValues(flow = createDelegate().observe(USER_WALLET_ID, jointCurrency, OPERATION_ID))
        advanceUntilIdle()

        // Assert
        assertThat(values).containsExactly(null)
        coVerify(exactly = 0) { jointAccountListFetcher(userWalletId = any()) }
    }

    @Test
    fun `GIVEN a currency outside a joint account WHEN observe THEN no supplier is touched`() = runTest {
        // Act
        val values = getEmittedValues(
            flow = createDelegate().observe(USER_WALLET_ID, nonJointCurrency, OPERATION_ID),
        )
        advanceUntilIdle()

        // Assert
        assertThat(values).containsExactly(null)
        verify(exactly = 0) { jointAccountListSupplier(USER_WALLET_ID) }
        coVerify(exactly = 0) { jointAccountListFetcher(userWalletId = any()) }
    }

    private fun createDelegate() = JointOperationDelegate(
        router = router,
        accountListSupplier = accountListSupplier,
        jointAccountListSupplier = jointAccountListSupplier,
        jointAccountListFetcher = jointAccountListFetcher,
        transferResolver = transferResolver,
        signersResolver = signersResolver,
        ownerAddressResolver = ownerAddressResolver,
        signJointOperationUseCase = signJointOperationUseCase,
        getWalletMetaInfoUseCase = getWalletMetaInfoUseCase,
        sendFeedbackEmailUseCase = sendFeedbackEmailUseCase,
        rejectJointOperationUseCase = rejectJointOperationUseCase,
        isAddressBookCompatibleUseCase = isAddressBookCompatibleUseCase,
        ownerLookupProducer = ownerLookupProducer,
        userWalletsListRepository = userWalletsListRepository,
        singleAccountStatusListSupplier = singleAccountStatusListSupplier,
        getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
    )

    private fun createJointAccount(
        cryptoAccountId: String = portfolioAccount.accountId.value,
        membersCount: Int = MEMBERS_COUNT,
        threshold: Int = THRESHOLD,
        safeAddress: String? = SAFE_ADDRESS,
        status: JointAccount.Status = JointAccount.Status.ACTIVE,
        members: List<JointAccount.Member> = listOf(MEMBER),
        source: StatusSource = StatusSource.ACTUAL,
        inFlightOperations: Map<Long, JointOperation> = mapOf(CHAIN_ID to createOperation()),
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