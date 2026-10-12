package com.tangem.data.walletmanager

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.SafeEthereumWalletManager
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionStatus
import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.pagination.Page
import com.tangem.blockchain.common.pagination.PaginationWrapper
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.transactionhistory.TransactionHistoryState
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryRequest
import com.tangem.blockchainsdk.BlockchainSDKFactory
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.local.assets.AssetLoader
import com.tangem.data.walletmanager.SafeTestFixtures.OWNER_PATH
import com.tangem.data.walletmanager.SafeTestFixtures.SAFE_ADDRESS
import com.tangem.data.walletmanager.SafeTestFixtures.jointNetwork
import com.tangem.data.walletmanager.SafeTestFixtures.userWalletId
import com.tangem.data.walletmanager.store.WalletManagersStore
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.jointaccount.address.JointAccountSafeAddressResolver
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.GaslessTransactionRepository
import com.tangem.domain.txhistory.TxHistoryFeatureToggles
import com.tangem.domain.txhistory.models.TxHistoryState
import com.tangem.domain.txhistory.models.TxHistoryStateError
import com.tangem.domain.txhistory.usecase.GetCachedTokenInfoUseCase
import com.tangem.domain.walletmanager.model.SmartContractMethod
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Calendar

/**
 * A joint network's addresses, explorer link and transaction history come from its Safe wallet manager, the same
 * way every other network's come from its manager. Before the account has a Safe address there is no manager, and
 * the reads say "not ready" rather than failing.
 */
internal class DefaultWalletManagersFacadeJointReadTest {

    private val walletManagersStore: WalletManagersStore = mockk(relaxUnitFun = true)
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val safeAddressResolver: JointAccountSafeAddressResolver = mockk()
    private val sdkFactory: WalletManagerFactory = mockk()
    private val blockchainSDKFactory: BlockchainSDKFactory = mockk {
        coEvery { getWalletManagerFactorySync() } returns sdkFactory
    }
    private val manager: SafeEthereumWalletManager = SafeTestFixtures.safeManager()
    private val assetLoader: AssetLoader = mockk()
    private val gaslessTransactionRepository: GaslessTransactionRepository = mockk()
    private val getCachedTokenInfoUseCase: GetCachedTokenInfoUseCase = mockk()
    private val txHistoryFeatureToggles: TxHistoryFeatureToggles = mockk(relaxed = true)

    private val facade = DefaultWalletManagersFacade(
        walletManagersStore = walletManagersStore,
        userWalletsListRepository = userWalletsListRepository,
        assetLoader = assetLoader,
        dispatchers = TestingCoroutineDispatcherProvider(),
        gaslessTransactionRepository = gaslessTransactionRepository,
        jointAccountSafeAddressResolver = { safeAddressResolver },
        getCachedTokenInfoUseCase = getCachedTokenInfoUseCase,
        txHistoryFeatureToggles = txHistoryFeatureToggles,
        blockchainSDKFactory = blockchainSDKFactory,
    )

    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()
    private val jointEthereum = cryptoCurrencyFactory.ethereum.copy(network = jointNetwork)

    @BeforeEach
    fun resetMocks() {
        clearMocks(walletManagersStore, sdkFactory, safeAddressResolver, manager)
        every { manager.wallet } returns SafeTestFixtures.safeWallet()

        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(SafeTestFixtures.hotWallet()))
        coEvery { walletManagersStore.getSyncOrNull(userWalletId, Blockchain.Ethereum, OWNER_PATH) } returns manager
        coEvery { assetLoader.loadMap<SmartContractMethod>(fileName = any(), type = any()) } returns emptyMap()
        coEvery { gaslessTransactionRepository.getGaslessFeeAddresses() } returns emptySet()
        coEvery { getCachedTokenInfoUseCase.invoke(any<UserWalletId>(), any<String>()) } returns emptySet()
    }

    @Test
    fun `GIVEN a Safe manager WHEN getAddresses THEN the Safe address`() = runTest {
        // Act
        val actual = facade.getAddresses(userWalletId, jointNetwork)

        // Assert
        assertThat(actual.map { it.value to it.type }).containsExactly(SAFE_ADDRESS to AddressType.Default)
        assertThat(facade.getDefaultAddress(userWalletId, jointNetwork)).isEqualTo(SAFE_ADDRESS)
        verify { safeAddressResolver wasNot Called }
    }

    @Test
    fun `GIVEN a Safe manager WHEN getTxHistoryState THEN the history of the Safe address`() = runTest {
        // Arrange
        coEvery {
            manager.getTransactionHistoryState(SAFE_ADDRESS, TransactionHistoryRequest.FilterType.Coin)
        } returns TransactionHistoryState.Success.HasTransactions(txCount = 3)

        // Act
        val actual = facade.getTxHistoryState(userWalletId, jointEthereum)

        // Assert
        assertThat(actual).isEqualTo(TxHistoryState.Success.HasTransactions(txCount = 3))
    }

    @Test
    fun `GIVEN no Safe address yet WHEN getAddresses and getRecentTransactions THEN empty without the SDK`() = runTest {
        // Arrange
        noSafeAddressYet()

        // Act
        val addresses = facade.getAddresses(userWalletId, jointNetwork)
        val transactions = facade.getRecentTransactions(userWalletId, jointEthereum)

        // Assert
        assertThat(addresses).isEmpty()
        assertThat(transactions).isEmpty()
        verify { sdkFactory wasNot Called }
    }

    @Test
    fun `GIVEN no Safe address yet WHEN getTxHistoryState THEN the address is pending instead of a failure`() =
        runTest {
            // Arrange
            noSafeAddressYet()

            // Act
            val actual = facade.getTxHistoryState(userWalletId, jointEthereum)

            // Assert
            assertThat(actual).isEqualTo(TxHistoryState.AddressNotReady)
        }

    @Test
    fun `GIVEN no Safe address yet WHEN getTxHistoryItems THEN it is refused as pending, not as a data error`() =
        runTest {
            // Arrange
            noSafeAddressYet()

            // Act
            val actual = runCatching {
                facade.getTxHistoryItems(userWalletId, jointEthereum, Page.Initial, pageSize = 20)
            }.exceptionOrNull()

            // Assert
            assertThat(actual).isInstanceOf(TxHistoryStateError.AddressNotReady::class.java)
        }

    @Test
    fun `GIVEN a Safe manager WHEN getTxHistoryItems THEN the fetched items come back`() = runTest {
        // Arrange
        val wrapper: PaginationWrapper<TransactionHistoryItem> = mockk {
            every { nextPage } returns Page.LastPage
            every { items } returns listOf(historyItem())
        }
        coEvery { manager.getTransactionsHistory(any()) } returns Result.Success(wrapper)

        // Act
        val actual = facade.getTxHistoryItems(userWalletId, jointEthereum, Page.Initial, pageSize = 20)

        // Assert
        assertThat(actual.items.map { it.txHash }).containsExactly("0xhash")
    }

    @Test
    fun `GIVEN a personal network without a wallet manager WHEN getTxHistoryState THEN it still fails loudly`() =
        runTest {
            // Arrange
            val personal = cryptoCurrencyFactory.ethereum
            coEvery { walletManagersStore.getSyncOrNull(any(), any(), any()) } returns null
            every { userWalletsListRepository.userWallets } returns MutableStateFlow(null)

            // Act
            val actual = runCatching { facade.getTxHistoryState(userWalletId, personal) }.exceptionOrNull()

            // Assert
            assertThat(actual).isInstanceOf(IllegalArgumentException::class.java)
        }

    @Test
    fun `GIVEN a Safe manager WHEN getExploreUrl THEN the explorer link of the Safe address`() = runTest {
        // Act
        val actual = facade.getExploreUrl(userWalletId, jointNetwork, AddressType.Default, contractAddress = null)

        // Assert
        assertThat(actual).isEqualTo(Blockchain.Ethereum.getExploreUrl(SAFE_ADDRESS, null))
    }

    @Test
    fun `GIVEN the Safe sent a transaction WHEN getRecentTransactions THEN outgoing`() = runTest {
        // Arrange
        manager.wallet.recentTransactions.add(
            TransactionData.Uncompiled(
                amount = Amount(BigDecimal.ONE, Blockchain.Ethereum),
                fee = null,
                sourceAddress = SAFE_ADDRESS,
                destinationAddress = "0x2",
                status = TransactionStatus.Unconfirmed,
                hash = "hash",
                date = Calendar.getInstance().apply { timeInMillis = 1748251839317 },
                extras = null,
                contractAddress = null,
            ),
        )

        // Act
        val actual = facade.getRecentTransactions(userWalletId, jointEthereum)

        // Assert
        assertThat(actual.map { it.txHash to it.isOutgoing }).containsExactly("hash" to true)
    }

    private fun noSafeAddressYet() {
        coEvery { walletManagersStore.getSyncOrNull(userWalletId, Blockchain.Ethereum, OWNER_PATH) } returns null
        coEvery { safeAddressResolver.resolveByOwnerPath(userWalletId, setOf(OWNER_PATH)) } returns emptyMap()
    }

    private fun historyItem() = TransactionHistoryItem(
        txHash = "0xhash",
        timestamp = 0L,
        isOutgoing = true,
        destinationType = TransactionHistoryItem.DestinationType.Single(
            addressType = TransactionHistoryItem.AddressType.User("0x2"),
        ),
        sourceType = TransactionHistoryItem.SourceType.Single(SAFE_ADDRESS),
        status = TransactionHistoryItem.TransactionStatus.Confirmed,
        type = TransactionHistoryItem.TransactionType.Transfer,
        amount = Amount(BigDecimal.ONE, Blockchain.Ethereum),
        fee = Amount(BigDecimal.ZERO, Blockchain.Ethereum),
    )
}