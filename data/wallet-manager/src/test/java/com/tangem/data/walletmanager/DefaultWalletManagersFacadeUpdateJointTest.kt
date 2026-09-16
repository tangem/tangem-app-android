package com.tangem.data.walletmanager

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.ExternalAddressBalanceProvider
import com.tangem.blockchain.blockchains.ethereum.ExternalAddressInfo
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchainsdk.BlockchainSDKFactory
import com.tangem.blockchainsdk.models.UpdateWalletManagerResult
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.data.walletmanager.store.WalletManagersStore
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * The balance of a joint account is read by its Safe address through a provider the SDK builds without a key.
 * The provider is cached per wallet and network like a wallet manager, but never among the wallet managers.
 */
internal class DefaultWalletManagersFacadeUpdateJointTest {

    private val walletManagersStore: WalletManagersStore = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val sdkFactory: WalletManagerFactory = mockk()
    private val blockchainSDKFactory: BlockchainSDKFactory = mockk {
        coEvery { getWalletManagerFactorySync() } returns sdkFactory
    }
    private val provider: ExternalAddressBalanceProvider = mockk(relaxUnitFun = true)
    private val providersStore = ExternalAddressBalanceProvidersStore()

    private val facade = DefaultWalletManagersFacade(
        walletManagersStore = walletManagersStore,
        externalAddressBalanceProvidersStore = providersStore,
        userWalletsListRepository = userWalletsListRepository,
        assetLoader = mockk(),
        dispatchers = TestingCoroutineDispatcherProvider(),
        gaslessTransactionRepository = mockk(),
        jointAccountSafeAddressResolver = { mockk() },
        getCachedTokenInfoUseCase = mockk(),
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
    private val usdt = cryptoCurrencyFactory.createToken(Blockchain.Ethereum, id = "tether", contractAddress = "0xdAC1")
    private val sdkUsdt = Token(
        id = "tether",
        name = usdt.name,
        symbol = usdt.symbol,
        contractAddress = "0xdAC1",
        decimals = usdt.decimals,
    )

    private val safeAddresses = setOf(
        UpdateWalletManagerResult.Address(SAFE_ADDRESS, UpdateWalletManagerResult.Address.Type.Primary),
    )

    @BeforeEach
    fun resetMocks() = runTest {
        clearMocks(sdkFactory, provider)
        providersStore.remove(userWalletId, setOf(jointNetwork))

        every { sdkFactory.createExternalAddressBalanceProvider(Blockchain.Ethereum, SAFE_ADDRESS) } returns provider
        every { provider.address } returns SAFE_ADDRESS
        every { provider.tokens } returns emptySet()
        coEvery { provider.update() } returns Result.Success(
            ExternalAddressInfo(
                address = SAFE_ADDRESS,
                amounts = mapOf(AmountType.Coin to Amount(BigDecimal.ONE, Blockchain.Ethereum)),
                recentTransactions = emptyList(),
            ),
        )
    }

    @Test
    fun `GIVEN a provider answers WHEN updateJoint THEN verified by the Safe address and the provider is cached`() =
        runTest {
            // Act
            val actual = facade.updateJoint(userWalletId, jointNetwork, extraTokens = setOf(usdt), SAFE_ADDRESS)

            // Assert
            val expected = UpdateWalletManagerResult.Verified(
                selectedAddress = SAFE_ADDRESS,
                addresses = safeAddresses,
                currenciesAmounts = setOf(UpdateWalletManagerResult.CryptoCurrencyAmount.Coin(BigDecimal.ONE)),
                currentTransactions = emptySet(),
            )
            assertThat(actual).isEqualTo(expected)
            assertThat(providersStore.getSyncOrNull(userWalletId, jointNetwork)).isSameInstanceAs(provider)
            verify(exactly = 1) { provider.addTokens(listOf(sdkUsdt)) }
            verify {
                walletManagersStore wasNot Called
                userWalletsListRepository wasNot Called
            }
        }

    @Test
    fun `GIVEN a cached provider on the same address WHEN updateJoint twice THEN the SDK builds it once`() = runTest {
        // Act
        facade.updateJoint(userWalletId, jointNetwork, extraTokens = emptySet(), SAFE_ADDRESS)
        facade.updateJoint(userWalletId, jointNetwork, extraTokens = emptySet(), SAFE_ADDRESS)

        // Assert
        verify(exactly = 1) { sdkFactory.createExternalAddressBalanceProvider(Blockchain.Ethereum, SAFE_ADDRESS) }
        coVerify(exactly = 2) { provider.update() }
    }

    @Test
    fun `GIVEN the provider fails WHEN updateJoint THEN unreachable with the Safe address`() = runTest {
        // Arrange
        coEvery { provider.update() } returns Result.Failure(BlockchainSdkError.FailedToLoadFee)

        // Act
        val actual = facade.updateJoint(userWalletId, jointNetwork, extraTokens = emptySet(), SAFE_ADDRESS)

        // Assert
        val expected = UpdateWalletManagerResult.Unreachable(selectedAddress = SAFE_ADDRESS, addresses = safeAddresses)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN the SDK cannot serve the network WHEN updateJoint THEN unreachable with the Safe address`() = runTest {
        // Arrange
        every { sdkFactory.createExternalAddressBalanceProvider(Blockchain.Ethereum, SAFE_ADDRESS) } returns null

        // Act
        val actual = facade.updateJoint(userWalletId, jointNetwork, extraTokens = emptySet(), SAFE_ADDRESS)

        // Assert
        val expected = UpdateWalletManagerResult.Unreachable(selectedAddress = SAFE_ADDRESS, addresses = safeAddresses)
        assertThat(actual).isEqualTo(expected)
        assertThat(providersStore.getSyncOrNull(userWalletId, jointNetwork)).isNull()
    }

    @Test
    fun `GIVEN joint networks WHEN remove THEN their providers are dropped`() = runTest {
        // Arrange
        coEvery { walletManagersStore.remove(userWalletId, any()) } returns Unit
        facade.updateJoint(userWalletId, jointNetwork, extraTokens = emptySet(), SAFE_ADDRESS)

        // Act
        facade.remove(userWalletId, setOf(jointNetwork))

        // Assert
        assertThat(providersStore.getSyncOrNull(userWalletId, jointNetwork)).isNull()
    }

    private companion object {
        const val SAFE_ADDRESS = "0x5afe000000000000000000000000000000000001"
    }
}