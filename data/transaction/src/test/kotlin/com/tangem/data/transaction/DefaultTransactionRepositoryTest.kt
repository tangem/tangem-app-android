package com.tangem.data.transaction

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSender
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.smartcontract.CompiledSmartContractCallData
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.common.transaction.TransactionsSendResult
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplySendCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyUpgradeToAndCallCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.unwrapYieldModuleUpgrade
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
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
            coEvery {
                walletManagersFacade.wrapYieldSendWithModuleUpgrade(
                    userWalletId = userWalletId,
                    network = network,
                    callData = any(),
                )
            } answers {
                EthereumYieldSupplyUpgradeToAndCallCallData(
                    newImplementation = LATEST_IMPLEMENTATION,
                    innerCallData = thirdArg(),
                )
            }
        }

        @Test
        fun `GIVEN yield send and toggle on WHEN createTransferTransaction THEN sends the call wrapped by the facade`() =
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
        fun `GIVEN yield send and toggle off WHEN createTransferTransaction THEN bare send without upgrade decision`() =
            runTest {
                // Arrange
                val repository = createRepository(isToggleEnabled = false)

                // Act
                val actual = repository.createTransfer(amount = yieldSupplyAmount)

                // Assert
                assertThat(actual.callData()).isInstanceOf(EthereumYieldSupplySendCallData::class.java)
                coVerify(exactly = 0) { walletManagersFacade.wrapYieldSendWithModuleUpgrade(any(), any(), any()) }
            }

        @Test
        fun `GIVEN token transfer and toggle on WHEN createTransferTransaction THEN not wrapped`() = runTest {
            // Arrange
            val repository = createRepository(isToggleEnabled = true)

            // Act
            val actual = repository.createTransfer(amount = Amount(token = token, value = BigDecimal.ONE))

            // Assert
            assertThat(actual.callData()).isNotInstanceOf(EthereumYieldSupplyUpgradeToAndCallCallData::class.java)
            coVerify(exactly = 0) { walletManagersFacade.wrapYieldSendWithModuleUpgrade(any(), any(), any()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SendMultipleTransactions {

        private val sender: WalletManager = mockk(moreInterfaces = arrayOf(TransactionSender::class))
        private val signer: TransactionSigner = mockk()

        @Suppress("DEPRECATION")
        @BeforeEach
        fun resetMocks() {
            clearMocks(walletManagersFacade, sender)

            coEvery {
                walletManagersFacade.getOrCreateWalletManager(
                    userWalletId = userWalletId,
                    blockchain = Blockchain.Ethereum,
                    derivationPath = network.derivationPath.value,
                )
            } returns sender
            every { walletManagersFacade.resetYieldSendUpgradeDecision(any(), any()) } returns Unit
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN sent transactions WHEN sendMultipleTransactions THEN resets upgrade decision after a sent upgrade`(
            model: SendModel,
        ) = runTest {
            // Arrange
            val transaction = TransactionData.Uncompiled(
                amount = Amount(token = token, value = BigDecimal.ONE),
                sourceAddress = OWNER_ADDRESS,
                destinationAddress = MODULE_ADDRESS,
                fee = null,
                extras = EthereumTransactionExtras(callData = model.callData),
            )
            coEvery {
                (sender as TransactionSender).sendMultiple(
                    transactionDataList = listOf(transaction),
                    signer = signer,
                    sendMode = TransactionSender.MultipleTransactionSendMode.DEFAULT,
                )
            } returns model.sendResult

            // Act
            createRepository(isToggleEnabled = true).sendMultipleTransactions(
                txsData = listOf(transaction),
                signer = signer,
                userWalletId = userWalletId,
                network = network,
                sendMode = TransactionSender.MultipleTransactionSendMode.DEFAULT,
            )

            // Assert
            verify(exactly = if (model.isDecisionReset) 1 else 0) {
                walletManagersFacade.resetYieldSendUpgradeDecision(userWalletId, network)
            }
        }

        private fun provideTestModels() = listOf(
            SendModel(callData = upgradeWrappedCallData, sendResult = sendSuccess, isDecisionReset = true),
            SendModel(callData = bareCallData, sendResult = sendSuccess, isDecisionReset = false),
            SendModel(
                callData = upgradeWrappedCallData,
                sendResult = Result.Failure(BlockchainSdkError.FailedToSendException),
                isDecisionReset = false,
            ),
        )
    }

    data class SendModel(
        val callData: SmartContractCallData,
        val sendResult: Result<TransactionsSendResult>,
        val isDecisionReset: Boolean,
    )

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

        val bareCallData = CompiledSmartContractCallData(byteArrayOf(1, 2, 3))
        val upgradeWrappedCallData = EthereumYieldSupplyUpgradeToAndCallCallData(
            newImplementation = LATEST_IMPLEMENTATION,
            innerCallData = bareCallData,
        )
        val sendSuccess = Result.Success(TransactionsSendResult(hashes = listOf("0xhash")))

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