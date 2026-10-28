package com.tangem.data.yield.supply

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.blockchains.ethereum.network.EthereumTransactionReceipt
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionReceiptProvider
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.common.smartcontract.SmartContractCallDataProviderFactory
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.yieldsupply.YieldSupplyContractCallDataProviderFactory
import com.tangem.blockchain.yieldsupply.providers.YieldModuleVersionStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.utils.convertToSdkAmount
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.yield.supply.models.YieldSupplyEnterOutcome
import com.tangem.domain.yield.supply.models.YieldSupplyTransactionOutcome
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.math.BigInteger
import com.tangem.blockchain.yieldsupply.providers.YieldSupplyStatus as SDKYieldSupplyStatus

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultYieldSupplyTransactionRepositoryTest {

    private val networkId = Network.ID(value = "ethereum/test", derivationPath = Network.DerivationPath.None)
    private val mockedContractAddress = "0x000000000000000000000000000000000000"
    private val yieldContractAddress = "0x1234"

    private val userWalletId = mockk<UserWalletId>()
    private val cryptoCurrency = mockk<CryptoCurrency.Token>(relaxed = true) {
        every { network.id } returns networkId
        every { network.rawId } returns networkId.rawId.value
        every { contractAddress } returns mockedContractAddress
    }
    private val cryptoCurrencyStatus = mockk<CryptoCurrencyStatus>(relaxed = true) {
        every { currency } returns cryptoCurrency
        every { value.yieldSupplyStatus } returns null
        every { value.amount } returns BigDecimal.TEN
    }

    private val walletManager = mockk<WalletManager>(relaxed = true) {
        every { wallet } returns mockk(relaxed = true)
        every { getYieldSupplyContractAddresses() } returns mockk(relaxed = true) {
            every { factoryContractAddress } returns "factory"
        }
    }
    private val walletManagersFacade: WalletManagersFacade = mockk {
        coEvery { getOrCreateWalletManager(any(), any(), any()) } returns walletManager
    }
    private lateinit var repository: DefaultYieldSupplyTransactionRepository
    private val receiptWalletManager = mockk<WalletManager>(
        relaxed = true,
        moreInterfaces = arrayOf(TransactionReceiptProvider::class),
    )
    private val outcomeWalletManagersFacade: WalletManagersFacade = mockk()
    private val outcomeRepository = DefaultYieldSupplyTransactionRepository(
        walletManagersFacade = outcomeWalletManagersFacade,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun setUp() {
        repository = spyk(
            objToCopy = DefaultYieldSupplyTransactionRepository(
                walletManagersFacade = walletManagersFacade,
                dispatchers = TestingCoroutineDispatcherProvider(),
            ),
            recordPrivateCalls = true,
        )
        every { mockk<Token>().contractAddress } returns mockedContractAddress
        clearMocks(receiptWalletManager, outcomeWalletManagersFacade)
    }

    @Test
    fun `createEnterTransactions returns deploy-approve-enter transactions`() = runTest {
        coEvery { walletManager.getYieldModuleAddress() } returns EthereumUtils.ZERO_ADDRESS
        coEvery { walletManager.getYieldSupplyStatus(any()) } returns null
        coEvery { walletManager.isAllowedToSpend(any()) } returns false
        coEvery { walletManager.calculateYieldModuleAddress() } returns yieldContractAddress

        val result = repository.createEnterTransactions(
            userWalletId = userWalletId,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            maxNetworkFee = BigDecimal.TEN,
        )

        // Assert that 3 transactions are returned: deploy, approve, enter
        Truth.assertThat(result).isNotNull()
        Truth.assertThat(result).isNotEmpty()

        // Check transaction - deploy
        val firstExpectedCallData = YieldSupplyContractCallDataProviderFactory.getDeployCallData(
            walletAddress = walletManager.wallet.address,
            tokenContractAddress = mockedContractAddress,
            maxNetworkFee = BigDecimal.TEN.convertToSdkAmount(cryptoCurrencyStatus),
        )
        val firstTransaction = result.first()

        Truth.assertThat(firstTransaction.extras).isInstanceOf(EthereumTransactionExtras::class.java)
        Truth.assertThat((firstTransaction.extras as EthereumTransactionExtras).callData?.data)
            .isEqualTo(firstExpectedCallData.data)

        // Check transaction - approve
        val secondExpectedCallData = SmartContractCallDataProviderFactory.getApprovalCallData(
            spenderAddress = yieldContractAddress,
            amount = null,
            blockchain = Blockchain.EthereumTestnet,
        )
        val secondTransaction = result[1]

        Truth.assertThat(secondTransaction.extras).isInstanceOf(EthereumTransactionExtras::class.java)
        Truth.assertThat((secondTransaction.extras as EthereumTransactionExtras).callData?.data)
            .isEqualTo(secondExpectedCallData.data)

        // Check transaction - enter
        val thirdExpectedCallData = YieldSupplyContractCallDataProviderFactory.getEnterCallData(mockedContractAddress)
        val thirdTransaction = result[2]

        Truth.assertThat(thirdTransaction.extras).isInstanceOf(EthereumTransactionExtras::class.java)
        Truth.assertThat((thirdTransaction.extras as EthereumTransactionExtras).callData?.data)
            .isEqualTo(thirdExpectedCallData.data)
    }

    @Test
    fun `createEnterTransactions returns init-approve-enter transactions`() = runTest {
        coEvery { walletManager.getYieldModuleAddress() } returns yieldContractAddress
        coEvery { walletManager.calculateYieldModuleAddress() } returns yieldContractAddress
        coEvery { walletManager.isAllowedToSpend(any()) } returns false
        coEvery { walletManager.getYieldSupplyStatus(any()) } returns SDKYieldSupplyStatus(
            isActive = false,
            isInitialized = false,
            maxNetworkFee = BigDecimal.TEN,
        )

        val result = repository.createEnterTransactions(
            userWalletId = userWalletId,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            maxNetworkFee = BigDecimal.TEN,
        )

        // Assert that 3 transactions are returned: init token, approve, enter
        Truth.assertThat(result).isNotNull()
        Truth.assertThat(result).isNotEmpty()

        // Check transaction - init token
        val firstExpectedCallData = YieldSupplyContractCallDataProviderFactory.getInitTokenCallData(
            tokenContractAddress = mockedContractAddress,
            maxNetworkFee = BigDecimal.TEN.convertToSdkAmount(cryptoCurrencyStatus),
        )
        val firstTransaction = result.first()

        Truth.assertThat(firstTransaction.extras).isInstanceOf(EthereumTransactionExtras::class.java)
        Truth.assertThat((firstTransaction.extras as EthereumTransactionExtras).callData?.data)
            .isEqualTo(firstExpectedCallData.data)

        // Check transaction - approve
        val secondExpectedCallData = SmartContractCallDataProviderFactory.getApprovalCallData(
            spenderAddress = yieldContractAddress,
            amount = null,
            blockchain = Blockchain.EthereumTestnet,
        )
        val secondTransaction = result[1]

        Truth.assertThat(secondTransaction.extras).isInstanceOf(EthereumTransactionExtras::class.java)
        Truth.assertThat((secondTransaction.extras as EthereumTransactionExtras).callData?.data)
            .isEqualTo(secondExpectedCallData.data)

        // Check transaction - enter
        val thirdExpectedCallData = YieldSupplyContractCallDataProviderFactory.getEnterCallData(mockedContractAddress)
        val thirdTransaction = result[2]

        Truth.assertThat(thirdTransaction.extras).isInstanceOf(EthereumTransactionExtras::class.java)
        Truth.assertThat((thirdTransaction.extras as EthereumTransactionExtras).callData?.data)
            .isEqualTo(thirdExpectedCallData.data)
    }

    @Test
    fun `createEnterTransactions returns reactivate-approve-enter transactions`() = runTest {
        coEvery { walletManager.getYieldModuleAddress() } returns yieldContractAddress
        coEvery { walletManager.calculateYieldModuleAddress() } returns yieldContractAddress
        coEvery { walletManager.isAllowedToSpend(any()) } returns false
        coEvery { walletManager.getYieldSupplyStatus(any()) } returns SDKYieldSupplyStatus(
            isActive = false,
            isInitialized = true,
            maxNetworkFee = BigDecimal.TEN,
        )

        val result = repository.createEnterTransactions(
            userWalletId = userWalletId,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            maxNetworkFee = BigDecimal.TEN,
        )

        // Assert that 3 transactions are returned: reactivate token, approve, enter
        Truth.assertThat(result).isNotNull()
        Truth.assertThat(result).isNotEmpty()

        // Check transaction - reactivate token
        val firstExpectedCallData = YieldSupplyContractCallDataProviderFactory.getReactivateTokenCallData(
            tokenContractAddress = mockedContractAddress,
            maxNetworkFee = BigDecimal.TEN.convertToSdkAmount(cryptoCurrencyStatus),
        )
        val firstTransaction = result.first()

        Truth.assertThat(firstTransaction.extras).isInstanceOf(EthereumTransactionExtras::class.java)
        Truth.assertThat((firstTransaction.extras as EthereumTransactionExtras).callData?.data)
            .isEqualTo(firstExpectedCallData.data)

        // Check transaction - approve
        val secondExpectedCallData = SmartContractCallDataProviderFactory.getApprovalCallData(
            spenderAddress = yieldContractAddress,
            amount = null,
            blockchain = Blockchain.EthereumTestnet,
        )
        val secondTransaction = result[1]

        Truth.assertThat(secondTransaction.extras).isInstanceOf(EthereumTransactionExtras::class.java)
        Truth.assertThat((secondTransaction.extras as EthereumTransactionExtras).callData?.data)
            .isEqualTo(secondExpectedCallData.data)

        // Check transaction - enter
        val thirdExpectedCallData = YieldSupplyContractCallDataProviderFactory.getEnterCallData(mockedContractAddress)
        val thirdTransaction = result[2]

        Truth.assertThat(thirdTransaction.extras).isInstanceOf(EthereumTransactionExtras::class.java)
        Truth.assertThat((thirdTransaction.extras as EthereumTransactionExtras).callData?.data)
            .isEqualTo(thirdExpectedCallData.data)
    }

    @Test
    fun `createEnterTransactions returns reactivate-enter transactions`() = runTest {
        coEvery { walletManager.getYieldModuleAddress() } returns yieldContractAddress
        coEvery { walletManager.calculateYieldModuleAddress() } returns yieldContractAddress
        coEvery { walletManager.getYieldSupplyStatus(any()) } returns SDKYieldSupplyStatus(
            isActive = false,
            isInitialized = true,
            maxNetworkFee = BigDecimal.TEN,
        )
        coEvery { walletManager.isAllowedToSpend(any()) } returns true

        val result = repository.createEnterTransactions(
            userWalletId = userWalletId,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            maxNetworkFee = BigDecimal.TEN,
        )

        // Assert that 2 transactions are returned: approve, enter
        Truth.assertThat(result).isNotNull()
        Truth.assertThat(result).isNotEmpty()

        // Check transaction - reactivate token
        val firstExpectedCallData = YieldSupplyContractCallDataProviderFactory.getReactivateTokenCallData(
            tokenContractAddress = mockedContractAddress,
            maxNetworkFee = BigDecimal.TEN.convertToSdkAmount(cryptoCurrencyStatus),
        )
        val firstTransaction = result.first()

        Truth.assertThat(firstTransaction.extras).isInstanceOf(EthereumTransactionExtras::class.java)
        Truth.assertThat((firstTransaction.extras as EthereumTransactionExtras).callData?.data)
            .isEqualTo(firstExpectedCallData.data)

        // Check transaction - enter
        val thirdExpectedCallData = YieldSupplyContractCallDataProviderFactory.getEnterCallData(mockedContractAddress)
        val thirdTransaction = result[1]

        Truth.assertThat(thirdTransaction.extras).isInstanceOf(EthereumTransactionExtras::class.java)
        Truth.assertThat((thirdTransaction.extras as EthereumTransactionExtras).callData?.data)
            .isEqualTo(thirdExpectedCallData.data)
    }

    @Test
    fun `createEnterTransactions returns enter transactions`() = runTest {
        coEvery { walletManager.getYieldModuleAddress() } returns yieldContractAddress
        coEvery { walletManager.calculateYieldModuleAddress() } returns yieldContractAddress
        coEvery { walletManager.getYieldSupplyStatus(any()) } returns SDKYieldSupplyStatus(
            isActive = true,
            isInitialized = true,
            maxNetworkFee = BigDecimal.TEN,
        )
        coEvery { walletManager.isAllowedToSpend(any()) } returns true

        val result = repository.createEnterTransactions(
            userWalletId = userWalletId,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            maxNetworkFee = BigDecimal.TEN,
        )

        // Assert that transaction is returned: enter
        Truth.assertThat(result).isNotNull()
        Truth.assertThat(result).isNotEmpty()

        // Check transaction - enter
        val thirdExpectedCallData = YieldSupplyContractCallDataProviderFactory.getEnterCallData(mockedContractAddress)
        val thirdTransaction = result[0]

        Truth.assertThat(thirdTransaction.extras).isInstanceOf(EthereumTransactionExtras::class.java)
        Truth.assertThat((thirdTransaction.extras as EthereumTransactionExtras).callData?.data)
            .isEqualTo(thirdExpectedCallData.data)
    }

    @Test
    fun `createExitTransaction returns valid transaction`() = runTest {
        val expectedCallData =
            YieldSupplyContractCallDataProviderFactory.getExitCallData(mockedContractAddress)

        val result = repository.createExitTransaction(userWalletId, cryptoCurrencyStatus, null)

        Truth.assertThat(result).isNotNull()
        Truth.assertThat(result.extras).isInstanceOf(EthereumTransactionExtras::class.java)
        Truth.assertThat((result.extras as EthereumTransactionExtras).callData?.data).isEqualTo(expectedCallData.data)
    }

    @Test
    fun `createPartialWithdrawCallData returns withdraw call data when module is up to date`() = runTest {
        coEvery { walletManager.checkModuleVersionStatus() } returns YieldModuleVersionStatus.UpToDate

        val token = mockk<CryptoCurrency.Token>(relaxed = true) {
            every { contractAddress } returns mockedContractAddress
            every { decimals } returns 6
        }
        val amount = Amount(
            currencySymbol = "USDC",
            value = BigDecimal("1.5"),
            decimals = 6,
            type = AmountType.Token(
                token = Token(
                    symbol = "USDC",
                    contractAddress = mockedContractAddress,
                    decimals = 6,
                ),
            ),
        )

        val result = repository.createPartialWithdrawCallData(
            userWalletId = userWalletId,
            cryptoCurrency = token,
            amount = amount,
        )

        Truth.assertThat(result).isNotNull()
        Truth.assertThat(result.methodId).isEqualTo("0xf3fef3a3")
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CreateClaimRewardsTransaction {

        private val callData = mockk<SmartContractCallData>()

        @Test
        fun `GIVEN no fee WHEN createClaimRewardsTransaction THEN builds a zero amount call to the module`() = runTest {
            // Act
            val result = repository.createClaimRewardsTransaction(
                userWalletId = userWalletId,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                destinationAddress = MODULE_ADDRESS,
                callData = callData,
                fee = null,
            )

            // Assert
            Truth.assertThat(result.destinationAddress).isEqualTo(MODULE_ADDRESS)
            Truth.assertThat(result.amount.value).isEqualTo(BigDecimal.ZERO)
            Truth.assertThat(result.fee).isNull()
            Truth.assertThat((result.extras as EthereumTransactionExtras).callData).isSameInstanceAs(callData)
        }

        @Test
        fun `GIVEN fee WHEN createClaimRewardsTransaction THEN wallet manager builds a zero amount call to the module`() =
            runTest {
                // Arrange
                val fee = mockk<Fee>()
                val amount = slot<Amount>()
                every {
                    walletManager.createTransaction(amount = capture(amount), fee = fee, destination = MODULE_ADDRESS)
                } answers {
                    TransactionData.Uncompiled(
                        amount = amount.captured,
                        fee = fee,
                        sourceAddress = "0xowner",
                        destinationAddress = MODULE_ADDRESS,
                    )
                }

                // Act
                val result = repository.createClaimRewardsTransaction(
                    userWalletId = userWalletId,
                    cryptoCurrencyStatus = cryptoCurrencyStatus,
                    destinationAddress = MODULE_ADDRESS,
                    callData = callData,
                    fee = fee,
                )

                // Assert
                Truth.assertThat(amount.captured.value).isEqualTo(BigDecimal.ZERO)
                Truth.assertThat(result.destinationAddress).isEqualTo(MODULE_ADDRESS)
                Truth.assertThat(result.fee).isSameInstanceAs(fee)
                Truth.assertThat((result.extras as EthereumTransactionExtras).callData).isSameInstanceAs(callData)
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetEnterTransactionOutcome {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN receipt WHEN getEnterTransactionOutcome THEN maps outcome`(model: OutcomeModel) = runTest {
            // Arrange
            val resolvedWalletManager = if (model.isReceiptProvider) receiptWalletManager else walletManager
            coEvery {
                outcomeWalletManagersFacade.getOrCreateWalletManager(any(), any(), any())
            } returns resolvedWalletManager
            coEvery {
                (receiptWalletManager as TransactionReceiptProvider).getTransactionReceipt(ENTER_TX_HASH)
            } returns model.receipt
            coEvery { receiptWalletManager.getYieldSupplyStatus(any()) } returns SDKYieldSupplyStatus(
                isActive = model.protocolBalance != null,
                isInitialized = true,
                maxNetworkFee = BigDecimal.TEN,
            )
            coEvery { receiptWalletManager.getEffectiveProtocolBalance(any()) } returns
                (model.protocolBalance ?: BigDecimal.ZERO)

            // Act
            val actual = outcomeRepository.getEnterTransactionOutcome(
                userWalletId = userWalletId,
                cryptoCurrency = cryptoCurrency,
                transactionHash = ENTER_TX_HASH,
            )

            // Assert
            Truth.assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            OutcomeModel(receipt = Result.Success(null), expected = null),
            OutcomeModel(receipt = Result.Failure(BlockchainSdkError.CustomError("rpc down")), expected = null),
            OutcomeModel(
                receipt = Result.Success(receipt(transactionHash = ENTER_TX_HASH, isSuccess = true)),
                expected = YieldSupplyEnterOutcome.Deposited,
            ),
            OutcomeModel(
                receipt = Result.Success(receipt(transactionHash = ENTER_TX_HASH, isSuccess = false)),
                protocolBalance = null,
                expected = YieldSupplyEnterOutcome.Failed,
            ),
            OutcomeModel(
                receipt = Result.Success(receipt(transactionHash = ENTER_TX_HASH, isSuccess = false)),
                protocolBalance = BigDecimal.TEN,
                expected = YieldSupplyEnterOutcome.DepositedByRelayer,
            ),
            OutcomeModel(
                receipt = Result.Success(receipt(transactionHash = ENTER_TX_HASH, isSuccess = true)),
                isReceiptProvider = false,
                expected = null,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetTransactionOutcome {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN receipt WHEN getTransactionOutcome THEN maps outcome`(model: TransactionOutcomeModel) = runTest {
            // Arrange
            val resolvedWalletManager = if (model.isReceiptProvider) receiptWalletManager else walletManager
            coEvery {
                outcomeWalletManagersFacade.getOrCreateWalletManager(any(), any(), any())
            } returns resolvedWalletManager
            coEvery {
                (receiptWalletManager as TransactionReceiptProvider).getTransactionReceipt(CLAIM_TX_HASH)
            } returns model.receipt

            // Act
            val actual = outcomeRepository.getTransactionOutcome(
                userWalletId = userWalletId,
                cryptoCurrency = cryptoCurrency,
                transactionHash = CLAIM_TX_HASH,
            )

            // Assert
            Truth.assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            TransactionOutcomeModel(receipt = Result.Success(null), expected = null),
            TransactionOutcomeModel(
                receipt = Result.Failure(BlockchainSdkError.CustomError("rpc down")),
                expected = null,
            ),
            TransactionOutcomeModel(
                receipt = Result.Success(receipt(transactionHash = CLAIM_TX_HASH, isSuccess = true)),
                expected = YieldSupplyTransactionOutcome.Confirmed,
            ),
            TransactionOutcomeModel(
                receipt = Result.Success(receipt(transactionHash = CLAIM_TX_HASH, isSuccess = false)),
                expected = YieldSupplyTransactionOutcome.Reverted,
            ),
            TransactionOutcomeModel(
                receipt = Result.Success(receipt(transactionHash = CLAIM_TX_HASH, isSuccess = true)),
                isReceiptProvider = false,
                expected = null,
            ),
        )
    }

    data class TransactionOutcomeModel(
        val receipt: Result<EthereumTransactionReceipt?>,
        val expected: YieldSupplyTransactionOutcome?,
        val isReceiptProvider: Boolean = true,
    )

    data class OutcomeModel(
        val receipt: Result<EthereumTransactionReceipt?>,
        val expected: YieldSupplyEnterOutcome?,
        val protocolBalance: BigDecimal? = null,
        val isReceiptProvider: Boolean = true,
    )

    private fun receipt(transactionHash: String, isSuccess: Boolean) = EthereumTransactionReceipt(
        transactionHash = transactionHash,
        isSuccess = isSuccess,
        blockNumber = BigInteger.ONE,
        gasUsed = BigInteger.ONE,
    )

    private companion object {
        const val ENTER_TX_HASH = "0xenter"
        const val CLAIM_TX_HASH = "0xclaim"
        const val MODULE_ADDRESS = "0x2222222222222222222222222222222222222222"
    }
}