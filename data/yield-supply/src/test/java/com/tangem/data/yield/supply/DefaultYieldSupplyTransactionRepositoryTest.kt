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
import com.tangem.blockchain.yieldsupply.providers.YieldModuleUpgradeUnavailableException
import com.tangem.blockchain.yieldsupply.providers.YieldModuleVersionStatus
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyUpgradeToAndCallCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.MerklClaimCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.unwrapYieldModuleUpgrade
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
import io.mockk.coVerify
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
import org.junit.jupiter.params.provider.ValueSource
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
        yieldModuleFeatureToggles = mockk { every { isYieldModuleV3Enabled } returns false },
    )

    @BeforeEach
    fun setUp() {
        repository = spyk(
            objToCopy = DefaultYieldSupplyTransactionRepository(
                walletManagersFacade = walletManagersFacade,
                dispatchers = TestingCoroutineDispatcherProvider(),
                yieldModuleFeatureToggles = mockk { every { isYieldModuleV3Enabled } returns false },
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
    inner class SupportsEnterWithAmount {

        @ParameterizedTest
        @ValueSource(booleans = [true, false])
        fun `GIVEN module V2 check WHEN supportsEnterWithAmount THEN returns it`(isAtLeastV2: Boolean) = runTest {
            // Arrange
            val versionStatus = YieldModuleVersionStatus.UpgradeUnavailable(currentImplementation = OLD_IMPLEMENTATION)
            coEvery { walletManager.checkModuleVersionStatus() } returns versionStatus
            every { walletManager.isAtLeastV2(versionStatus) } returns isAtLeastV2

            // Act
            val actual = repository.supportsEnterWithAmount(userWalletId, cryptoCurrency.network)

            // Assert
            Truth.assertThat(actual).isEqualTo(isAtLeastV2)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class WrapYieldSwapCallDataWithUpgradeIfNeeded {

        private val callData = YieldSupplyContractCallDataProviderFactory.getExitCallData(
            tokenContractAddress = mockedContractAddress,
        )

        @Test
        fun `GIVEN UpgradeAvailable WHEN wrap THEN upgradeToAndCall with original inner`() = runTest {
            // Arrange
            coEvery { walletManager.checkModuleVersionStatus() } returns YieldModuleVersionStatus.UpgradeAvailable(
                currentImplementation = OLD_IMPLEMENTATION,
                latestImplementation = LATEST_IMPLEMENTATION,
            )

            // Act
            val actual = repository.wrapYieldSwapCallDataWithUpgradeIfNeeded(
                userWalletId = userWalletId,
                network = cryptoCurrency.network,
                callData = callData,
            )

            // Assert
            Truth.assertThat(actual).isInstanceOf(EthereumYieldSupplyUpgradeToAndCallCallData::class.java)
            Truth.assertThat(actual.methodId).isEqualTo(EthereumYieldSupplyUpgradeToAndCallCallData.METHOD_ID)
            Truth.assertThat(actual.unwrapYieldModuleUpgrade()).isSameInstanceAs(callData)
        }

        @Test
        fun `GIVEN UpToDate WHEN wrap THEN returns original instance`() = runTest {
            // Arrange
            coEvery { walletManager.checkModuleVersionStatus() } returns YieldModuleVersionStatus.UpToDate

            // Act
            val actual = repository.wrapYieldSwapCallDataWithUpgradeIfNeeded(
                userWalletId = userWalletId,
                network = cryptoCurrency.network,
                callData = callData,
            )

            // Assert
            Truth.assertThat(actual).isSameInstanceAs(callData)
        }
    }

    @Test
    fun `GIVEN UpgradeAvailable WHEN createPartialWithdrawCallData THEN keeps withdraw call data as inner`() = runTest {
        // Arrange
        coEvery { walletManager.checkModuleVersionStatus() } returns YieldModuleVersionStatus.UpgradeAvailable(
            currentImplementation = OLD_IMPLEMENTATION,
            latestImplementation = LATEST_IMPLEMENTATION,
        )
        val token = mockk<CryptoCurrency.Token>(relaxed = true) {
            every { contractAddress } returns mockedContractAddress
        }
        val amount = Amount(
            currencySymbol = "USDC",
            value = BigDecimal("1.5"),
            decimals = 6,
            type = AmountType.Token(
                token = Token(symbol = "USDC", contractAddress = mockedContractAddress, decimals = 6),
            ),
        )

        // Act
        val actual = repository.createPartialWithdrawCallData(
            userWalletId = userWalletId,
            cryptoCurrency = token,
            amount = amount,
        )

        // Assert
        Truth.assertThat(actual.methodId).isEqualTo(EthereumYieldSupplyUpgradeToAndCallCallData.METHOD_ID)
        Truth.assertThat(actual.unwrapYieldModuleUpgrade().methodId).isEqualTo("0xf3fef3a3")
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CreatePartialWithdrawCallDataUpgradeUnavailable {

        private val versionStatus = YieldModuleVersionStatus.UpgradeUnavailable(currentImplementation = OLD_IMPLEMENTATION)
        private val token = mockk<CryptoCurrency.Token>(relaxed = true) {
            every { contractAddress } returns mockedContractAddress
        }
        private val amount = Amount(
            currencySymbol = "USDC",
            value = BigDecimal("1.5"),
            decimals = 6,
            type = AmountType.Token(
                token = Token(symbol = "USDC", contractAddress = mockedContractAddress, decimals = 6),
            ),
        )

        @Test
        fun `GIVEN V2 module the factory can't upgrade WHEN createPartialWithdrawCallData THEN withdraw is not wrapped`() =
            runTest {
                // Arrange
                coEvery { walletManager.checkModuleVersionStatus() } returns versionStatus
                every { walletManager.isAtLeastV2(versionStatus) } returns true

                // Act
                val actual = repository.createPartialWithdrawCallData(
                    userWalletId = userWalletId,
                    cryptoCurrency = token,
                    amount = amount,
                )

                // Assert
                Truth.assertThat(actual.methodId).isEqualTo("0xf3fef3a3")
            }

        @Test
        fun `GIVEN pre-V2 module the factory can't upgrade WHEN createPartialWithdrawCallData THEN throws`() = runTest {
            // Arrange
            coEvery { walletManager.checkModuleVersionStatus() } returns versionStatus
            every { walletManager.isAtLeastV2(versionStatus) } returns false

            // Act
            val actual = runCatching {
                repository.createPartialWithdrawCallData(
                    userWalletId = userWalletId,
                    cryptoCurrency = token,
                    amount = amount,
                )
            }.exceptionOrNull()

            // Assert
            Truth.assertThat(actual).isInstanceOf(YieldModuleUpgradeUnavailableException::class.java)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CreateClaimRewardsTransaction {

        private val callData = mockk<SmartContractCallData>()
        private val claimCallData = MerklClaimCallData(data = byteArrayOf(0x2e, 0x7b, 0xa6.toByte(), 0xef.toByte()))
        private val upgradeAvailable = YieldModuleVersionStatus.UpgradeAvailable(
            currentImplementation = OLD_IMPLEMENTATION,
            latestImplementation = LATEST_IMPLEMENTATION,
        )
        private val v3Repository = DefaultYieldSupplyTransactionRepository(
            walletManagersFacade = walletManagersFacade,
            dispatchers = TestingCoroutineDispatcherProvider(),
            yieldModuleFeatureToggles = mockk { every { isYieldModuleV3Enabled } returns true },
        )

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

        @Test
        fun `GIVEN V3 disabled and UpgradeAvailable WHEN createClaimRewardsTransaction THEN claim is not wrapped`() =
            runTest {
                // Arrange
                coEvery { walletManager.checkModuleVersionStatus() } returns upgradeAvailable

                // Act
                val result = repository.createClaimRewardsTransaction(
                    userWalletId = userWalletId,
                    cryptoCurrencyStatus = cryptoCurrencyStatus,
                    destinationAddress = MODULE_ADDRESS,
                    callData = claimCallData,
                    fee = null,
                )

                // Assert
                Truth.assertThat((result.extras as EthereumTransactionExtras).callData).isSameInstanceAs(claimCallData)
            }

        @Test
        fun `GIVEN V3 enabled and UpgradeAvailable WHEN createClaimRewardsTransaction THEN claim is wrapped`() =
            runTest {
                // Arrange
                coEvery { walletManager.checkModuleVersionStatus() } returns upgradeAvailable

                // Act
                val result = v3Repository.createClaimRewardsTransaction(
                    userWalletId = userWalletId,
                    cryptoCurrencyStatus = cryptoCurrencyStatus,
                    destinationAddress = MODULE_ADDRESS,
                    callData = claimCallData,
                    fee = null,
                )

                // Assert
                val actual = (result.extras as EthereumTransactionExtras).callData
                Truth.assertThat(actual).isInstanceOf(EthereumYieldSupplyUpgradeToAndCallCallData::class.java)
                Truth.assertThat(actual?.unwrapYieldModuleUpgrade()).isSameInstanceAs(claimCallData)
            }

        @Test
        fun `GIVEN V3 enabled and UpToDate WHEN createClaimRewardsTransaction THEN claim is not wrapped`() = runTest {
            // Arrange
            coEvery { walletManager.checkModuleVersionStatus() } returns YieldModuleVersionStatus.UpToDate

            // Act
            val result = v3Repository.createClaimRewardsTransaction(
                userWalletId = userWalletId,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                destinationAddress = MODULE_ADDRESS,
                callData = claimCallData,
                fee = null,
            )

            // Assert
            Truth.assertThat((result.extras as EthereumTransactionExtras).callData).isSameInstanceAs(claimCallData)
        }

        @Test
        fun `GIVEN V3 enabled and UpgradeUnavailable WHEN createClaimRewardsTransaction THEN throws`() = runTest {
            // Arrange
            coEvery { walletManager.checkModuleVersionStatus() } returns YieldModuleVersionStatus.UpgradeUnavailable(
                currentImplementation = OLD_IMPLEMENTATION,
            )

            // Act
            val actual = runCatching {
                v3Repository.createClaimRewardsTransaction(
                    userWalletId = userWalletId,
                    cryptoCurrencyStatus = cryptoCurrencyStatus,
                    destinationAddress = MODULE_ADDRESS,
                    callData = claimCallData,
                    fee = null,
                )
            }.exceptionOrNull()

            // Assert
            Truth.assertThat(actual).isInstanceOf(YieldModuleUpgradeUnavailableException::class.java)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CreateEnterTransactionsUpgrade {

        private val upgradeAvailable = YieldModuleVersionStatus.UpgradeAvailable(
            currentImplementation = OLD_IMPLEMENTATION,
            latestImplementation = LATEST_IMPLEMENTATION,
        )
        private val v3Repository = DefaultYieldSupplyTransactionRepository(
            walletManagersFacade = walletManagersFacade,
            dispatchers = TestingCoroutineDispatcherProvider(),
            yieldModuleFeatureToggles = mockk { every { isYieldModuleV3Enabled } returns true },
        )

        @BeforeEach
        fun clearWalletManagerCalls() {
            clearMocks(walletManager, answers = false)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun createEnterTransactions(model: EnterUpgradeModel) = runTest {
            // Arrange
            arrangeWalletManager(model.tokenState)
            coEvery { walletManager.checkModuleVersionStatus() } returns model.moduleStatus

            // Act
            val result = v3Repository.createEnterTransactions(
                userWalletId = userWalletId,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                maxNetworkFee = BigDecimal.TEN,
            )

            // Assert
            val actual = result.map { (it.extras as EthereumTransactionExtras).callData }
            Truth.assertThat(actual.map { it is EthereumYieldSupplyUpgradeToAndCallCallData })
                .containsExactlyElementsIn(model.expectedWrapped)
                .inOrder()
            val wrapped = actual.filterIsInstance<EthereumYieldSupplyUpgradeToAndCallCallData>()
            wrapped.forEach { Truth.assertThat(it.newImplementation).isEqualTo(LATEST_IMPLEMENTATION) }
            Truth.assertThat(wrapped.map { it.innerCallData.methodId }).isEqualTo(listOfNotNull(model.expectedInnerMethodId))
            coVerify(exactly = if (model.tokenState == TokenState.NOT_DEPLOYED) 0 else 1) {
                walletManager.checkModuleVersionStatus()
            }
        }

        @Test
        fun `GIVEN V3 disabled and UpgradeAvailable WHEN createEnterTransactions THEN no wrap and no check`() =
            runTest {
                // Arrange
                arrangeWalletManager(TokenState.NOT_INITIALIZED)
                coEvery { walletManager.checkModuleVersionStatus() } returns upgradeAvailable

                // Act
                val result = repository.createEnterTransactions(
                    userWalletId = userWalletId,
                    cryptoCurrencyStatus = cryptoCurrencyStatus,
                    maxNetworkFee = BigDecimal.TEN,
                )

                // Assert
                val actual = result.map { (it.extras as EthereumTransactionExtras).callData }
                Truth.assertThat(actual.none { it is EthereumYieldSupplyUpgradeToAndCallCallData }).isTrue()
                coVerify(exactly = 0) { walletManager.checkModuleVersionStatus() }
            }

        @Test
        fun `GIVEN version check throws WHEN createEnterTransactions THEN batch is not wrapped`() = runTest {
            // Arrange
            arrangeWalletManager(TokenState.NOT_INITIALIZED)
            coEvery { walletManager.checkModuleVersionStatus() } throws IllegalStateException("rpc failed")

            // Act
            val result = v3Repository.createEnterTransactions(
                userWalletId = userWalletId,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                maxNetworkFee = BigDecimal.TEN,
            )

            // Assert
            val actual = result.map { (it.extras as EthereumTransactionExtras).callData }
            Truth.assertThat(actual).hasSize(3)
            Truth.assertThat(actual.none { it is EthereumYieldSupplyUpgradeToAndCallCallData }).isTrue()
        }

        private fun arrangeWalletManager(tokenState: TokenState) {
            coEvery { walletManager.calculateYieldModuleAddress() } returns yieldContractAddress
            coEvery { walletManager.getYieldModuleAddress() } returns when (tokenState) {
                TokenState.NOT_DEPLOYED -> EthereumUtils.ZERO_ADDRESS
                else -> yieldContractAddress
            }
            coEvery { walletManager.isAllowedToSpend(any()) } returns (tokenState == TokenState.ACTIVE_APPROVED)
            coEvery { walletManager.getYieldSupplyStatus(any()) } returns when (tokenState) {
                TokenState.NOT_DEPLOYED -> null
                else -> SDKYieldSupplyStatus(
                    isActive = tokenState == TokenState.ACTIVE_APPROVED,
                    isInitialized = tokenState != TokenState.NOT_INITIALIZED,
                    maxNetworkFee = BigDecimal.TEN,
                )
            }
        }

        private fun provideTestModels() = listOf(
            EnterUpgradeModel(
                tokenState = TokenState.NOT_INITIALIZED,
                moduleStatus = upgradeAvailable,
                expectedWrapped = listOf(true, false, false),
                expectedInnerMethodId = YieldSupplyContractCallDataProviderFactory.getInitTokenCallData(
                    tokenContractAddress = mockedContractAddress,
                    maxNetworkFee = BigDecimal.TEN.convertToSdkAmount(cryptoCurrencyStatus),
                ).methodId,
            ),
            EnterUpgradeModel(
                tokenState = TokenState.INACTIVE,
                moduleStatus = upgradeAvailable,
                expectedWrapped = listOf(true, false, false),
                expectedInnerMethodId = YieldSupplyContractCallDataProviderFactory.getReactivateTokenCallData(
                    tokenContractAddress = mockedContractAddress,
                    maxNetworkFee = BigDecimal.TEN.convertToSdkAmount(cryptoCurrencyStatus),
                ).methodId,
            ),
            EnterUpgradeModel(
                tokenState = TokenState.ACTIVE_APPROVED,
                moduleStatus = upgradeAvailable,
                expectedWrapped = listOf(true),
                expectedInnerMethodId = YieldSupplyContractCallDataProviderFactory.getEnterCallData(
                    tokenContractAddress = mockedContractAddress,
                ).methodId,
            ),
            EnterUpgradeModel(
                tokenState = TokenState.NOT_INITIALIZED,
                moduleStatus = YieldModuleVersionStatus.UpgradeUnavailable(currentImplementation = OLD_IMPLEMENTATION),
                expectedWrapped = listOf(false, false, false),
            ),
            EnterUpgradeModel(
                tokenState = TokenState.NOT_DEPLOYED,
                moduleStatus = YieldModuleVersionStatus.NotDeployed,
                expectedWrapped = listOf(false, false, false),
            ),
        )
    }

    enum class TokenState { NOT_DEPLOYED, NOT_INITIALIZED, INACTIVE, ACTIVE_APPROVED }

    data class EnterUpgradeModel(
        val tokenState: TokenState,
        val moduleStatus: YieldModuleVersionStatus,
        val expectedWrapped: List<Boolean>,
        val expectedInnerMethodId: String? = null,
    )

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
        const val OLD_IMPLEMENTATION = "0x4444444444444444444444444444444444444444"
        const val LATEST_IMPLEMENTATION = "0x5555555555555555555555555555555555555555"
    }
}