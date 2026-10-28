package com.tangem.data.transaction

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.yieldsupply.providers.YieldModuleVersionStatus
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplySendCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyUpgradeToAndCallCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.unwrapYieldModuleUpgrade
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

internal class DefaultTransactionRepositoryTest {

    private val walletManagersFacade: WalletManagersFacade = mockk()
    private val walletManager: WalletManager = mockk()
    private val wallet: Wallet = mockk()

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CreateTransferTransaction {

        @Suppress("DEPRECATION")
        @BeforeEach
        fun resetMocks() {
            clearMocks(walletManagersFacade, walletManager, wallet)

            coEvery {
                walletManagersFacade.getOrCreateWalletManager(
                    userWalletId = userWalletId,
                    blockchain = Blockchain.Ethereum,
                    derivationPath = network.derivationPath.value,
                )
            } returns walletManager
            every { walletManager.wallet } returns wallet
            every { wallet.address } returns OWNER_ADDRESS
            coEvery { walletManager.getYieldModuleAddress() } returns MODULE_ADDRESS
            coEvery { walletManager.checkModuleVersionStatus() } returns YieldModuleVersionStatus.UpgradeAvailable(
                currentImplementation = CURRENT_IMPLEMENTATION,
                latestImplementation = LATEST_IMPLEMENTATION,
            )
        }

        @Test
        fun `GIVEN yield send and toggle on and upgrade available WHEN createTransferTransaction THEN send is wrapped`() =
            runTest {
                // Arrange
                val repository = createRepository(isToggleEnabled = true)

                // Act
                val actual = repository.createTransfer(amount = yieldSupplyAmount)

                // Assert
                val callData = actual.callData()
                assertThat(callData).isInstanceOf(EthereumYieldSupplyUpgradeToAndCallCallData::class.java)
                assertThat(callData.unwrapYieldModuleUpgrade())
                    .isInstanceOf(EthereumYieldSupplySendCallData::class.java)
            }

        @Test
        fun `GIVEN yield send and toggle off WHEN createTransferTransaction THEN bare send without version check`() =
            runTest {
                // Arrange
                val repository = createRepository(isToggleEnabled = false)

                // Act
                val actual = repository.createTransfer(amount = yieldSupplyAmount)

                // Assert
                assertThat(actual.callData()).isInstanceOf(EthereumYieldSupplySendCallData::class.java)
                coVerify(exactly = 0) { walletManager.checkModuleVersionStatus() }
            }

        @Test
        fun `GIVEN token transfer and toggle on WHEN createTransferTransaction THEN not wrapped`() = runTest {
            // Arrange
            val repository = createRepository(isToggleEnabled = true)

            // Act
            val actual = repository.createTransfer(amount = Amount(token = token, value = BigDecimal.ONE))

            // Assert
            assertThat(actual.callData()).isNotInstanceOf(EthereumYieldSupplyUpgradeToAndCallCallData::class.java)
            coVerify(exactly = 0) { walletManager.checkModuleVersionStatus() }
        }
    }

    private fun createRepository(isToggleEnabled: Boolean) = DefaultTransactionRepository(
        tangemTechApi = mockk(),
        walletManagersFacade = walletManagersFacade,
        dispatchers = TestingCoroutineDispatcherProvider(),
        yieldModuleFeatureToggles = mockk { every { isYieldModuleV3Enabled } returns isToggleEnabled },
    )

    private suspend fun DefaultTransactionRepository.createTransfer(amount: Amount): TransactionData.Uncompiled =
        createTransferTransaction(
            amount = amount,
            fee = null,
            memo = null,
            destination = DESTINATION_ADDRESS,
            userWalletId = userWalletId,
            network = network,
            nonce = null,
        )

    private fun TransactionData.Uncompiled.callData(): SmartContractCallData =
        requireNotNull((extras as EthereumTransactionExtras).callData)

    private companion object {
        const val OWNER_ADDRESS = "0x1111111111111111111111111111111111111111"
        const val MODULE_ADDRESS = "0x2222222222222222222222222222222222222222"
        const val DESTINATION_ADDRESS = "0x3333333333333333333333333333333333333333"
        const val CURRENT_IMPLEMENTATION = "0x4444444444444444444444444444444444444444"
        const val LATEST_IMPLEMENTATION = "0x5555555555555555555555555555555555555555"

        val userWalletId = UserWalletId(stringValue = "011")

        val network = Network(
            id = Network.ID(value = "ethereum", derivationPath = Network.DerivationPath.None),
            name = "Ethereum",
            currencySymbol = "ETH",
            derivationPath = Network.DerivationPath.None,
            isTestnet = false,
            standardType = Network.StandardType.ERC20,
            hasFiatFeeRate = false,
            canHandleTokens = true,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = Network.NameResolvingType.NONE,
        )

        val token = Token(
            name = "USD Coin",
            symbol = "USDC",
            contractAddress = "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48",
            decimals = 6,
        )

        val yieldSupplyAmount = Amount(
            currencySymbol = token.symbol,
            value = BigDecimal.ONE,
            decimals = token.decimals,
            type = AmountType.TokenYieldSupply(
                token = token,
                isActive = true,
                isInitialized = true,
                isAllowedToSpend = true,
            ),
        )
    }
}