package com.tangem.data.walletmanager

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.ExternalAddressBalanceProvider
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.transactionhistory.TransactionHistoryState
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryRequest
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem
import com.tangem.blockchain.common.pagination.PaginationWrapper
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.common.pagination.Page
import com.tangem.blockchainsdk.BlockchainSDKFactory
import com.tangem.core.local.assets.AssetLoader
import com.tangem.domain.transaction.GaslessTransactionRepository
import com.tangem.domain.walletmanager.model.SmartContractMethod
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.data.walletmanager.store.WalletManagersStore
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.jointaccount.address.JointAccountSafeAddressResolver
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.models.TxHistoryState
import com.tangem.domain.txhistory.models.TxHistoryStateError
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Calendar

/**
 * A joint network has no wallet manager: its addresses, explorer link and transaction history come from the
 * provider that reads the Safe address. The provider is the one the balance fetch cached; before the first fetch
 * it is built from the Safe address resolved out of the accounts.
 */
internal class DefaultWalletManagersFacadeJointReadTest {

    private val walletManagersStore: WalletManagersStore = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val safeAddressResolver: JointAccountSafeAddressResolver = mockk()
    private val sdkFactory: WalletManagerFactory = mockk()
    private val blockchainSDKFactory: BlockchainSDKFactory = mockk {
        coEvery { getWalletManagerFactorySync() } returns sdkFactory
    }
    private val provider: ExternalAddressBalanceProvider = mockk(relaxUnitFun = true)
    private val assetLoader: AssetLoader = mockk()
    private val gaslessTransactionRepository: GaslessTransactionRepository = mockk()
    private val providersStore = ExternalAddressBalanceProvidersStore()

    private val facade = DefaultWalletManagersFacade(
        walletManagersStore = walletManagersStore,
        externalAddressBalanceProvidersStore = providersStore,
        userWalletsListRepository = userWalletsListRepository,
        assetLoader = assetLoader,
        dispatchers = TestingCoroutineDispatcherProvider(),
        gaslessTransactionRepository = gaslessTransactionRepository,
        jointAccountSafeAddressResolver = { safeAddressResolver },
        blockchainSDKFactory = blockchainSDKFactory,
    )

    private val userWalletId = UserWalletId("011")
    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()
    private val jointNetwork = cryptoCurrencyFactory.ethereum.network.let { network ->
        val derivationPath = Network.DerivationPath.Joint(value = "m/44'/60'/888888'/0/2")

        network.copy(
            id = Network.ID(rawId = network.id.rawId, derivationPath = derivationPath),
            derivationPath = derivationPath,
        )
    }
    private val jointEthereum = cryptoCurrencyFactory.ethereum.copy(network = jointNetwork)

    @BeforeEach
    fun resetMocks() = runTest {
        clearMocks(sdkFactory, provider, safeAddressResolver)
        providersStore.remove(userWalletId, setOf(jointNetwork))

        every { sdkFactory.createExternalAddressBalanceProvider(Blockchain.Ethereum, SAFE_ADDRESS) } returns provider
        every { provider.blockchain } returns Blockchain.Ethereum
        every { provider.address } returns SAFE_ADDRESS
        every { provider.recentTransactions } returns emptyList()
        coEvery { assetLoader.loadMap<SmartContractMethod>(fileName = any(), type = any()) } returns emptyMap()
        coEvery { gaslessTransactionRepository.getGaslessFeeAddresses() } returns emptySet()
        coEvery { safeAddressResolver.resolve(userWalletId, setOf(jointNetwork)) } returns mapOf(jointNetwork to SAFE_ADDRESS)
    }

    @Test
    fun `GIVEN a cached provider WHEN getAddresses THEN the Safe address without resolving`() = runTest {
        // Arrange
        providersStore.store(userWalletId, jointNetwork, provider)

        // Act
        val actual = facade.getAddresses(userWalletId, jointNetwork)

        // Assert
        assertThat(actual.map { it.value to it.type }).containsExactly(SAFE_ADDRESS to AddressType.Default)
        assertThat(facade.getDefaultAddress(userWalletId, jointNetwork)).isEqualTo(SAFE_ADDRESS)
        coVerify {
            safeAddressResolver wasNot Called
            walletManagersStore wasNot Called
        }
    }

    @Test
    fun `GIVEN no cached provider WHEN getTxHistoryState THEN built from the resolved Safe address and cached`() =
        runTest {
            // Arrange
            coEvery {
                provider.getTransactionHistoryState(SAFE_ADDRESS, TransactionHistoryRequest.FilterType.Coin)
            } returns TransactionHistoryState.Success.HasTransactions(txCount = 3)

            // Act
            val actual = facade.getTxHistoryState(userWalletId, jointEthereum)

            // Assert
            assertThat(actual).isEqualTo(TxHistoryState.Success.HasTransactions(txCount = 3))
            assertThat(providersStore.getSyncOrNull(userWalletId, jointNetwork)).isSameInstanceAs(provider)
            verify(exactly = 1) { sdkFactory.createExternalAddressBalanceProvider(Blockchain.Ethereum, SAFE_ADDRESS) }
        }

    @Test
    fun `GIVEN no Safe address yet WHEN getAddresses and getRecentTransactions THEN empty without the SDK`() = runTest {
        // Arrange
        coEvery { safeAddressResolver.resolve(userWalletId, setOf(jointNetwork)) } returns emptyMap()

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
            coEvery { safeAddressResolver.resolve(userWalletId, setOf(jointNetwork)) } returns emptyMap()

            // Act
            val actual = facade.getTxHistoryState(userWalletId, jointEthereum)

            // Assert
            assertThat(actual).isEqualTo(TxHistoryState.AddressNotReady)
            verify { sdkFactory wasNot Called }
        }

    @Test
    fun `GIVEN no Safe address yet WHEN getTxHistoryItems THEN it is refused as pending, not as a data error`() =
        runTest {
            // Arrange
            coEvery { safeAddressResolver.resolve(userWalletId, setOf(jointNetwork)) } returns emptyMap()

            // Act
            val actual = runCatching {
                facade.getTxHistoryItems(userWalletId, jointEthereum, Page.Initial, pageSize = 20)
            }.exceptionOrNull()

            // Assert
            assertThat(actual).isInstanceOf(TxHistoryStateError.AddressNotReady::class.java)
        }

    /**
     * A joint network has no wallet manager by design, so requiring one after the provider has already answered
     * would drop every fetched item. Its tokens only name the approved token of an `approve`, and the viewed
     * currency is the documented fallback for a contract the wallet does not track.
     */
    @Test
    fun `GIVEN a resolved Safe address WHEN getTxHistoryItems THEN the fetched items come back`() = runTest {
        // Arrange
        providersStore.store(userWalletId, jointNetwork, provider)
        val wrapper: PaginationWrapper<TransactionHistoryItem> = mockk {
            every { nextPage } returns Page.LastPage
            every { items } returns listOf(historyItem())
        }
        coEvery { provider.getTransactionsHistory(any()) } returns Result.Success(wrapper)

        // Act
        val actual = facade.getTxHistoryItems(userWalletId, jointEthereum, Page.Initial, pageSize = 20)

        // Assert
        assertThat(actual.items.map { it.txHash }).containsExactly("0xhash")
        coVerify { walletManagersStore wasNot Called }
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

    @Test
    fun `GIVEN a personal network without a wallet manager WHEN getTxHistoryState THEN it still fails loudly`() =
        runTest {
            // Arrange
            val personal = cryptoCurrencyFactory.ethereum
            every { userWalletsListRepository.userWallets } returns MutableStateFlow(null)
            coEvery { walletManagersStore.getSyncOrNull(any(), any(), any()) } returns null

            // Act
            val actual = runCatching { facade.getTxHistoryState(userWalletId, personal) }.exceptionOrNull()

            // Assert
            assertThat(actual).isInstanceOf(IllegalArgumentException::class.java)
        }

    @Test
    fun `GIVEN a cached provider WHEN getExploreUrl THEN the explorer link of the Safe address`() = runTest {
        // Arrange
        providersStore.store(userWalletId, jointNetwork, provider)

        // Act
        val actual = facade.getExploreUrl(userWalletId, jointNetwork, AddressType.Default, contractAddress = null)

        // Assert
        assertThat(actual).isEqualTo(Blockchain.Ethereum.getExploreUrl(SAFE_ADDRESS, null))
        verify { walletManagersStore wasNot Called }
    }

    @Test
    fun `GIVEN the provider holds a transaction from the Safe WHEN getRecentTransactions THEN outgoing`() = runTest {
        // Arrange
        providersStore.store(userWalletId, jointNetwork, provider)
        every { provider.recentTransactions } returns listOf(
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

    private companion object {
        const val SAFE_ADDRESS = "0x5afe000000000000000000000000000000000001"
    }
}