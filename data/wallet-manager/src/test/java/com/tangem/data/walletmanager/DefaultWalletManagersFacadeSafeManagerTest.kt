package com.tangem.data.walletmanager

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.SafeEthereumWalletManager
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.blockchainsdk.BlockchainSDKFactory
import com.tangem.blockchainsdk.models.UpdateWalletManagerResult
import com.tangem.data.walletmanager.SafeTestFixtures.OWNER_PATH
import com.tangem.data.walletmanager.SafeTestFixtures.SAFE_ADDRESS
import com.tangem.data.walletmanager.SafeTestFixtures.jointNetwork
import com.tangem.data.walletmanager.SafeTestFixtures.userWalletId
import com.tangem.data.walletmanager.store.WalletManagersStore
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.jointaccount.address.JointAccountSafeAddressResolver
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * A joint network gets the Safe wallet manager: built once on the owner key, once the account has a Safe address,
 * and kept among the wallet managers under the owner path like any other manager. Without a Safe address there is
 * no manager and no error — the account is still pending.
 */
internal class DefaultWalletManagersFacadeSafeManagerTest {

    private val walletManagersStore: WalletManagersStore = mockk(relaxUnitFun = true)
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val safeAddressResolver: JointAccountSafeAddressResolver = mockk()
    private val sdkFactory: WalletManagerFactory = mockk()
    private val blockchainSDKFactory: BlockchainSDKFactory = mockk {
        coEvery { getWalletManagerFactorySync() } returns sdkFactory
    }
    private val manager: SafeEthereumWalletManager = SafeTestFixtures.safeManager()

    private val facade = DefaultWalletManagersFacade(
        walletManagersStore = walletManagersStore,
        userWalletsListRepository = userWalletsListRepository,
        assetLoader = mockk(),
        dispatchers = TestingCoroutineDispatcherProvider(),
        gaslessTransactionRepository = mockk(),
        jointAccountSafeAddressResolver = { safeAddressResolver },
        getCachedTokenInfoUseCase = mockk(),
        txHistoryFeatureToggles = mockk(relaxed = true),
        blockchainSDKFactory = blockchainSDKFactory,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(walletManagersStore, sdkFactory, safeAddressResolver)

        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(SafeTestFixtures.hotWallet()))
        coEvery { walletManagersStore.getSyncOrNull(userWalletId, Blockchain.Ethereum, OWNER_PATH) } returns null
        coEvery {
            safeAddressResolver.resolveByOwnerPath(userWalletId, setOf(OWNER_PATH))
        } returns mapOf(OWNER_PATH to SAFE_ADDRESS)
        every { sdkFactory.createSafeWalletManager(Blockchain.Ethereum, any(), SAFE_ADDRESS) } returns manager
    }

    @Test
    fun `GIVEN a Safe address WHEN getOrCreateWalletManager by network THEN the Safe manager is built and stored`() =
        runTest {
            // Act
            @Suppress("DEPRECATION")
            val actual = facade.getOrCreateWalletManager(userWalletId = userWalletId, network = jointNetwork)

            // Assert
            assertThat(actual).isSameInstanceAs(manager)
            coVerify(exactly = 1) {
                sdkFactory.createSafeWalletManager(Blockchain.Ethereum, any(), SAFE_ADDRESS)
                walletManagersStore.store(userWalletId, manager)
            }
        }

    @Test
    fun `GIVEN a Safe address WHEN getOrCreateWalletManager by raw owner path THEN the same Safe manager`() = runTest {
        // Act
        @Suppress("DEPRECATION")
        val actual = facade.getOrCreateWalletManager(
            userWalletId = userWalletId,
            blockchain = Blockchain.Ethereum,
            derivationPath = OWNER_PATH,
        )

        // Assert
        assertThat(actual).isSameInstanceAs(manager)
    }

    @Test
    fun `GIVEN a stored Safe manager WHEN getOrCreateWalletManager THEN neither the resolver nor the SDK is asked`() =
        runTest {
            // Arrange
            coEvery { walletManagersStore.getSyncOrNull(userWalletId, Blockchain.Ethereum, OWNER_PATH) } returns manager

            // Act
            @Suppress("DEPRECATION")
            val actual = facade.getOrCreateWalletManager(userWalletId = userWalletId, network = jointNetwork)

            // Assert
            assertThat(actual).isSameInstanceAs(manager)
            verify {
                safeAddressResolver wasNot Called
                sdkFactory wasNot Called
            }
        }

    @Test
    fun `GIVEN no Safe address yet WHEN getOrCreateWalletManager THEN null and nothing is stored`() = runTest {
        // Arrange
        coEvery { safeAddressResolver.resolveByOwnerPath(userWalletId, setOf(OWNER_PATH)) } returns emptyMap()

        // Act
        @Suppress("DEPRECATION")
        val actual = facade.getOrCreateWalletManager(userWalletId = userWalletId, network = jointNetwork)

        // Assert
        assertThat(actual).isNull()
        verify { sdkFactory wasNot Called }
        coVerify(exactly = 0) { walletManagersStore.store(any(), any()) }
    }

    @Test
    fun `GIVEN the SDK cannot build the Safe manager WHEN getOrCreateWalletManager THEN null and nothing stored`() =
        runTest {
            // Arrange
            every { sdkFactory.createSafeWalletManager(Blockchain.Ethereum, any(), SAFE_ADDRESS) } returns null

            // Act
            @Suppress("DEPRECATION")
            val actual = facade.getOrCreateWalletManager(userWalletId = userWalletId, network = jointNetwork)

            // Assert
            assertThat(actual).isNull()
            coVerify(exactly = 0) { walletManagersStore.store(any(), any()) }
        }

    @Test
    fun `GIVEN the owner key is not derived WHEN getOrCreateWalletManager THEN null instead of a failure`() = runTest {
        // Arrange
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(
            listOf(SafeTestFixtures.hotWallet(derivedPaths = emptySet())),
        )

        // Act
        @Suppress("DEPRECATION")
        val actual = facade.getOrCreateWalletManager(userWalletId = userWalletId, network = jointNetwork)

        // Assert
        assertThat(actual).isNull()
        coVerify(exactly = 0) { walletManagersStore.store(any(), any()) }
    }

    @Test
    fun `GIVEN the owner key is not derived WHEN update THEN the derivation is reported missing`() = runTest {
        // Arrange
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(
            listOf(SafeTestFixtures.hotWallet(derivedPaths = emptySet())),
        )

        // Act
        val actual = facade.update(userWalletId = userWalletId, network = jointNetwork, extraTokens = emptySet())

        // Assert
        assertThat(actual).isEqualTo(UpdateWalletManagerResult.MissedDerivation)
        verify { sdkFactory wasNot Called }
    }

    @Test
    fun `GIVEN a Safe manager WHEN update THEN the Safe balance is read like any other network`() = runTest {
        // Arrange
        manager.wallet.setAmount(Amount(BigDecimal.ONE, Blockchain.Ethereum, AmountType.Coin))

        // Act
        val actual = facade.update(userWalletId = userWalletId, network = jointNetwork, extraTokens = emptySet())

        // Assert
        val expected = UpdateWalletManagerResult.Verified(
            selectedAddress = SAFE_ADDRESS,
            addresses = setOf(
                UpdateWalletManagerResult.Address(SAFE_ADDRESS, UpdateWalletManagerResult.Address.Type.Primary),
            ),
            currenciesAmounts = setOf(UpdateWalletManagerResult.CryptoCurrencyAmount.Coin(BigDecimal.ONE)),
            currentTransactions = emptySet(),
        )
        assertThat(actual).isEqualTo(expected)
        coVerify(exactly = 1) { manager.update(any()) }
    }

    @Test
    fun `GIVEN no Safe address yet WHEN update THEN unreachable without touching the SDK`() = runTest {
        // Arrange
        coEvery { safeAddressResolver.resolveByOwnerPath(userWalletId, setOf(OWNER_PATH)) } returns emptyMap()

        // Act
        val actual = facade.update(userWalletId = userWalletId, network = jointNetwork, extraTokens = emptySet())

        // Assert
        assertThat(actual).isEqualTo(UpdateWalletManagerResult.Unreachable())
        verify { sdkFactory wasNot Called }
    }
}