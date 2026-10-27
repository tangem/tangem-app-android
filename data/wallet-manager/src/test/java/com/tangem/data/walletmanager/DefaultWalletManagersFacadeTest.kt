package com.tangem.data.walletmanager

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.smartcontract.CompiledSmartContractCallData
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.yieldsupply.providers.YieldModuleVersionStatus
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplySendCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyUpgradeToAndCallCallData
import com.tangem.blockchainsdk.models.UpdateWalletManagerResult
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.data.walletmanager.store.WalletManagersStore
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

/**
 * A joint network reads through the Safe wallet manager, so without the wallet behind it there is nothing to build:
 * an update is unreachable and a lookup gets `null`, in every build.
 */
internal class DefaultWalletManagersFacadeTest {

    private val walletManagersStore: WalletManagersStore = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()

    private val facade = DefaultWalletManagersFacade(
        walletManagersStore = walletManagersStore,
        userWalletsListRepository = userWalletsListRepository,
        assetLoader = mockk(),
        dispatchers = TestingCoroutineDispatcherProvider(),
        gaslessTransactionRepository = mockk(),
        jointAccountSafeAddressResolver = { mockk() },
        getCachedTokenInfoUseCase = mockk(),
        jointRefundReceiverProvider = { SafeTestFixtures.REFUND_RECEIVER },
        blockchainSDKFactory = mockk(),
        yieldModuleFeatureToggles = mockk { every { isYieldModuleV3Enabled } returns false },
    )

    private val userWalletId = UserWalletId("011")
    private val walletManager: EthereumWalletManager = mockk()
    private val ethereumNetwork = MockCryptoCurrencyFactory().ethereum.network
    private val yieldSupplyAmount = Amount(
        currencySymbol = TOKEN.symbol,
        value = BigDecimal.ONE,
        decimals = TOKEN.decimals,
        type = AmountType.TokenYieldSupply(
            token = TOKEN,
            isActive = true,
            isInitialized = true,
            isAllowedToSpend = true,
        ),
    )
    private val jointNetwork = MockCryptoCurrencyFactory().ethereum.network.let { network ->
        val derivationPath = Network.DerivationPath.Joint(value = "m/44'/60'/888888'/0/2")

        network.copy(
            id = Network.ID(rawId = network.id.rawId, derivationPath = derivationPath),
            derivationPath = derivationPath,
        )
    }

    @Test
    fun `GIVEN joint network of a wallet the app does not hold WHEN update THEN unreachable without a manager`() =
        runTest {
            // Arrange
            every { userWalletsListRepository.userWallets } returns MutableStateFlow(null)

            // Act
            val actual = facade.update(userWalletId = userWalletId, network = jointNetwork, extraTokens = emptySet())

            // Assert
            assertThat(actual).isEqualTo(UpdateWalletManagerResult.Unreachable())
            verify { walletManagersStore wasNot Called }
        }

    @Suppress("DEPRECATION")
    @Test
    fun `GIVEN joint network of a wallet the app does not hold WHEN getOrCreateWalletManager THEN null`() = runTest {
        // Arrange
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(null)

        // Act
        val actual = facade.getOrCreateWalletManager(userWalletId = userWalletId, network = jointNetwork)

        // Assert
        assertThat(actual).isNull()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class EstimateFee {

        private val sentCallData = slot<SmartContractCallData>()

        @BeforeEach
        fun resetMocks() {
            stubYieldModuleWalletManager()
            sentCallData.clear()

            coEvery {
                walletManager.estimateFee(amount = any(), destination = any(), callData = capture(sentCallData))
            } returns Result.Success(TransactionFee.Single(normal = Fee.Common(amount = yieldSupplyAmount)))
        }

        @Test
        fun `GIVEN yield send and toggle on and upgrade available WHEN estimateFee THEN estimates wrapped send`() =
            runTest {
                // Arrange
                val facade = createFacade(isToggleEnabled = true)

                // Act
                facade.estimateFee(
                    amount = yieldSupplyAmount,
                    userWalletId = userWalletId,
                    network = ethereumNetwork,
                    isYieldModuleUpgradeAllowed = true,
                )

                // Assert
                assertThat(sentCallData.captured).isInstanceOf(EthereumYieldSupplyUpgradeToAndCallCallData::class.java)
            }

        @Test
        fun `GIVEN yield send and upgrade not allowed WHEN estimateFee THEN estimates bare send`() = runTest {
            // Arrange
            val facade = createFacade(isToggleEnabled = true)

            // Act
            facade.estimateFee(
                amount = yieldSupplyAmount,
                userWalletId = userWalletId,
                network = ethereumNetwork,
                isYieldModuleUpgradeAllowed = false,
            )

            // Assert
            assertThat(sentCallData.captured).isInstanceOf(EthereumYieldSupplySendCallData::class.java)
            coVerify(exactly = 0) { walletManager.checkModuleVersionStatus() }
        }

        @Test
        fun `GIVEN yield send and toggle off WHEN estimateFee THEN estimates bare send without version check`() =
            runTest {
                // Arrange
                val facade = createFacade(isToggleEnabled = false)

                // Act
                facade.estimateFee(
                    amount = yieldSupplyAmount,
                    userWalletId = userWalletId,
                    network = ethereumNetwork,
                    isYieldModuleUpgradeAllowed = true,
                )

                // Assert
                assertThat(sentCallData.captured).isInstanceOf(EthereumYieldSupplySendCallData::class.java)
                coVerify(exactly = 0) { walletManager.checkModuleVersionStatus() }
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class WrapYieldSendWithModuleUpgrade {

        private val sendCallData = CompiledSmartContractCallData(byteArrayOf(1, 2, 3))

        @BeforeEach
        fun resetMocks() {
            stubYieldModuleWalletManager()

            coEvery {
                walletManager.estimateFee(amount = any(), destination = any(), callData = any())
            } returns Result.Success(TransactionFee.Single(normal = Fee.Common(amount = yieldSupplyAmount)))
        }

        @Test
        fun `GIVEN fee estimated wrapped and version check fails now WHEN wrap THEN wrapped as estimated`() = runTest {
            // Arrange
            val facade = createFacade(isToggleEnabled = true)
            facade.estimateFee(amount = yieldSupplyAmount, userWalletId = userWalletId, network = ethereumNetwork)
            coEvery { walletManager.checkModuleVersionStatus() } throws IllegalStateException("RPC is down")

            // Act
            val actual = facade.wrapYieldSendWithModuleUpgrade(userWalletId, ethereumNetwork, sendCallData)

            // Assert
            assertThat(actual).isInstanceOf(EthereumYieldSupplyUpgradeToAndCallCallData::class.java)
            val wrapped = actual as EthereumYieldSupplyUpgradeToAndCallCallData
            assertThat(wrapped.newImplementation).isEqualTo(LATEST_IMPLEMENTATION)
            assertThat(wrapped.innerCallData).isSameInstanceAs(sendCallData)
        }

        @Test
        fun `GIVEN fee estimated bare after failed check and check passes now WHEN wrap THEN stays bare`() = runTest {
            // Arrange
            val facade = createFacade(isToggleEnabled = true)
            coEvery { walletManager.checkModuleVersionStatus() } throws IllegalStateException("RPC is down")
            facade.estimateFee(amount = yieldSupplyAmount, userWalletId = userWalletId, network = ethereumNetwork)
            stubUpgradeAvailable()

            // Act
            val actual = facade.wrapYieldSendWithModuleUpgrade(userWalletId, ethereumNetwork, sendCallData)

            // Assert
            assertThat(actual).isSameInstanceAs(sendCallData)
        }

        @Test
        fun `GIVEN no fee estimate WHEN wrap THEN checks module version now`() = runTest {
            // Arrange
            val facade = createFacade(isToggleEnabled = true)

            // Act
            val actual = facade.wrapYieldSendWithModuleUpgrade(userWalletId, ethereumNetwork, sendCallData)

            // Assert
            assertThat(actual).isInstanceOf(EthereumYieldSupplyUpgradeToAndCallCallData::class.java)
            coVerify(exactly = 1) { walletManager.checkModuleVersionStatus() }
        }

        @Test
        fun `GIVEN fee estimated with upgrade not allowed WHEN wrap THEN checks module version now`() = runTest {
            // Arrange
            val facade = createFacade(isToggleEnabled = true)
            facade.estimateFee(
                amount = yieldSupplyAmount,
                userWalletId = userWalletId,
                network = ethereumNetwork,
                isYieldModuleUpgradeAllowed = false,
            )

            // Act
            val actual = facade.wrapYieldSendWithModuleUpgrade(userWalletId, ethereumNetwork, sendCallData)

            // Assert
            assertThat(actual).isInstanceOf(EthereumYieldSupplyUpgradeToAndCallCallData::class.java)
            coVerify(exactly = 1) { walletManager.checkModuleVersionStatus() }
        }
    }

    private fun stubYieldModuleWalletManager() {
        clearMocks(walletManagersStore, userWalletsListRepository, walletManager)

        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(SafeTestFixtures.hotWallet()))
        coEvery {
            walletManagersStore.getSyncOrNull(
                userWalletId = userWalletId,
                blockchain = Blockchain.Ethereum,
                derivationPath = ethereumNetwork.derivationPath.value,
            )
        } returns walletManager
        coEvery { walletManager.getYieldModuleAddress() } returns MODULE_ADDRESS
        stubUpgradeAvailable()
    }

    private fun stubUpgradeAvailable() {
        coEvery { walletManager.checkModuleVersionStatus() } returns YieldModuleVersionStatus.UpgradeAvailable(
            currentImplementation = CURRENT_IMPLEMENTATION,
            latestImplementation = LATEST_IMPLEMENTATION,
        )
    }

    private fun createFacade(isToggleEnabled: Boolean) = DefaultWalletManagersFacade(
        walletManagersStore = walletManagersStore,
        userWalletsListRepository = userWalletsListRepository,
        assetLoader = mockk(),
        dispatchers = TestingCoroutineDispatcherProvider(),
        gaslessTransactionRepository = mockk(),
        jointAccountSafeAddressResolver = { mockk() },
        getCachedTokenInfoUseCase = mockk(),
        jointRefundReceiverProvider = { SafeTestFixtures.REFUND_RECEIVER },
        blockchainSDKFactory = mockk(),
        yieldModuleFeatureToggles = mockk { every { isYieldModuleV3Enabled } returns isToggleEnabled },
    )

    private companion object {
        const val MODULE_ADDRESS = "0x2222222222222222222222222222222222222222"
        const val CURRENT_IMPLEMENTATION = "0x4444444444444444444444444444444444444444"
        const val LATEST_IMPLEMENTATION = "0x5555555555555555555555555555555555555555"

        val TOKEN = Token(
            name = "USD Coin",
            symbol = "USDC",
            contractAddress = "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48",
            decimals = 6,
        )
    }
}