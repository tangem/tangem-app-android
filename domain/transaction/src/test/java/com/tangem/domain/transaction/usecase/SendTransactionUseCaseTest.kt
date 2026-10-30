package com.tangem.domain.transaction.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSender
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.common.transaction.TransactionsSendResult
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyEnterCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyExitCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplySendCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyUpgradeToAndCallCallData
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.demo.models.DemoConfig
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.notifications.repository.PushNotificationsRepository
import com.tangem.domain.transaction.TransactionRepository
import com.tangem.domain.transaction.models.EventTransactionTypeDto
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SendTransactionUseCaseTest {

    private val demoConfig: DemoConfig = mockk()
    private val cardSdkConfigRepository: CardSdkConfigRepository = mockk(relaxed = true)
    private val transactionRepository: TransactionRepository = mockk()
    private val walletManagersFacade: WalletManagersFacade = mockk()
    private val singleNetworkStatusFetcher: SingleNetworkStatusFetcher = mockk(relaxed = true)
    private val pushNotificationsRepository: PushNotificationsRepository = mockk()
    private val signer: TransactionSigner = mockk()
    private val userWallet: UserWallet.Hot = mockk()
    private val network: Network = mockk()

    private val useCase = SendTransactionUseCase(
        demoConfig = demoConfig,
        cardSdkConfigRepository = cardSdkConfigRepository,
        transactionRepository = transactionRepository,
        walletManagersFacade = walletManagersFacade,
        singleNetworkStatusFetcher = singleNetworkStatusFetcher,
        parallelUpdatingScope = TestAppCoroutineScope(),
        getHotWalletSigner = { signer },
        pushNotificationsRepository = pushNotificationsRepository,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(transactionRepository, pushNotificationsRepository, userWallet)
        every { userWallet.walletId } returns UserWalletId("0123456789ABCDEF")
        coEvery { pushNotificationsRepository.isNotificationsEnabled(any()) } returns false
        coEvery {
            transactionRepository.sendMultipleTransactions(
                txsData = any(),
                signer = any(),
                userWalletId = any(),
                network = any(),
                sendMode = any(),
            )
        } returns Result.Success(TransactionsSendResult(listOf(TX_HASH)))
        coEvery {
            transactionRepository.sendTransactionHash(hash = any(), transactionType = any(), userAddress = any())
        } returns Unit
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN yield call data WHEN send THEN tx hash is reported with matching type`(model: HashReportModel) =
        runTest {
            // Arrange
            val txData = transactionData(model.callData)

            // Act
            useCase(txData = txData, userWallet = userWallet, network = network)

            // Assert
            coVerify(exactly = 1) {
                transactionRepository.sendTransactionHash(
                    hash = TX_HASH,
                    transactionType = model.expectedType,
                    userAddress = null,
                )
            }
        }

    private fun provideTestModels() = listOf(
        HashReportModel(callData = mockk<EthereumYieldSupplyEnterCallData>(), expectedType = EventTransactionTypeDto.DEPOSIT),
        HashReportModel(callData = mockk<EthereumYieldSupplySendCallData>(), expectedType = EventTransactionTypeDto.SEND),
        HashReportModel(callData = mockk<EthereumYieldSupplyExitCallData>(), expectedType = EventTransactionTypeDto.WITHDRAW),
        HashReportModel(
            callData = wrapped(mockk<EthereumYieldSupplyEnterCallData>()),
            expectedType = EventTransactionTypeDto.DEPOSIT,
        ),
        HashReportModel(
            callData = wrapped(mockk<EthereumYieldSupplySendCallData>()),
            expectedType = EventTransactionTypeDto.SEND,
        ),
    )

    private fun wrapped(inner: SmartContractCallData) = EthereumYieldSupplyUpgradeToAndCallCallData(
        newImplementation = NEW_IMPLEMENTATION,
        innerCallData = inner,
    )

    private fun transactionData(callData: SmartContractCallData) = TransactionData.Uncompiled(
        amount = Amount(currencySymbol = "ETH", value = BigDecimal.ONE, decimals = 18),
        fee = null,
        sourceAddress = "0xSource",
        destinationAddress = "0xDestination",
        extras = EthereumTransactionExtras(callData = callData),
    )

    data class HashReportModel(val callData: SmartContractCallData, val expectedType: EventTransactionTypeDto)

    private companion object {
        const val TX_HASH = "0xhash"
        const val NEW_IMPLEMENTATION = "0x5555555555555555555555555555555555555555"
    }
}