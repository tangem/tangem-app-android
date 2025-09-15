package com.tangem.data.yield.supply

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.smartcontract.SmartContractCallDataProviderFactory
import com.tangem.blockchain.yieldsupply.YieldSupplyContractCallDataProviderFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.domain.utils.convertToSdkAmount
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import com.tangem.blockchain.yieldsupply.providers.YieldSupplyStatus as SDKYieldSupplyStatus
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultYieldSupplyTransactionRepositoryTest {

    private val networkId = Network.ID(value = "ETH/test", derivationPath = Network.DerivationPath.None)
    private val mockedContractAddress = "0x000000000000000000000000000000000000"
    private val yieldContractAddress = "0x1234"

    private val userWalletId = mockk<UserWalletId>()
    private val cryptoCurrency = mockk<CryptoCurrency.Token>(relaxed = true) {
        every { network.id } returns networkId
        every { contractAddress } returns mockedContractAddress
    }
    private val cryptoCurrencyStatus = mockk<CryptoCurrencyStatus>(relaxed = true) {
        every { currency } returns cryptoCurrency
        every { value.yieldSupplyStatus } returns null
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
    }

    @Test
    fun `createEnterTransactions returns deploy-approve-enter transactions`() = runTest {
        coEvery { walletManager.getYieldContract() } returns EthereumUtils.ZERO_ADDRESS
        coEvery { walletManager.getYieldSupplyStatus(any()) } returns null
        coEvery { walletManager.isAllowedToSpend(any()) } returns false
        coEvery { walletManager.calculateYieldContract() } returns yieldContractAddress

        val result = repository.createEnterTransactions(userWalletId, cryptoCurrencyStatus)

        // Assert that 3 transactions are returned: deploy, approve, enter
        Truth.assertThat(result).isNotNull()
        Truth.assertThat(result).isNotEmpty()

        // Check transaction - deploy
        val firstExpectedCallData = YieldSupplyContractCallDataProviderFactory.getDeployCallData(
            walletAddress = walletManager.wallet.address,
            tokenContractAddress = mockedContractAddress,
            maxNetworkFee = BigDecimal.TEN.convertToSdkAmount(cryptoCurrency),
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
        coEvery { walletManager.getYieldContract() } returns yieldContractAddress
        coEvery { walletManager.calculateYieldContract() } returns yieldContractAddress
        coEvery { walletManager.getYieldSupplyStatus(any()) } returns SDKYieldSupplyStatus(
            isActive = false,
            isInitialized = false,
            maxNetworkFee = BigDecimal.TEN,
        )

        val result = repository.createEnterTransactions(userWalletId, cryptoCurrencyStatus)

        // Assert that 3 transactions are returned: init token, approve, enter
        Truth.assertThat(result).isNotNull()
        Truth.assertThat(result).isNotEmpty()

        // Check transaction - init token
        val firstExpectedCallData = YieldSupplyContractCallDataProviderFactory.getInitTokenCallData(
            tokenContractAddress = mockedContractAddress,
            maxNetworkFee = BigDecimal.TEN.convertToSdkAmount(cryptoCurrency),
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
        coEvery { walletManager.getYieldContract() } returns yieldContractAddress
        coEvery { walletManager.calculateYieldContract() } returns yieldContractAddress
        coEvery { walletManager.getYieldSupplyStatus(any()) } returns SDKYieldSupplyStatus(
            isActive = false,
            isInitialized = true,
            maxNetworkFee = BigDecimal.TEN,
        )

        val result = repository.createEnterTransactions(userWalletId, cryptoCurrencyStatus)

        // Assert that 3 transactions are returned: reactivate token, approve, enter
        Truth.assertThat(result).isNotNull()
        Truth.assertThat(result).isNotEmpty()

        // Check transaction - reactivate token
        val firstExpectedCallData = YieldSupplyContractCallDataProviderFactory.getReactivateTokenCallData(
            tokenContractAddress = mockedContractAddress,
            maxNetworkFee = BigDecimal.TEN.convertToSdkAmount(cryptoCurrency),
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
        coEvery { walletManager.getYieldContract() } returns yieldContractAddress
        coEvery { walletManager.calculateYieldContract() } returns yieldContractAddress
        coEvery { walletManager.getYieldSupplyStatus(any()) } returns SDKYieldSupplyStatus(
            isActive = false,
            isInitialized = true,
            maxNetworkFee = BigDecimal.TEN,
        )
        coEvery { walletManager.isAllowedToSpend(any()) } returns true

        val result = repository.createEnterTransactions(userWalletId, cryptoCurrencyStatus)

        // Assert that 2 transactions are returned: approve, enter
        Truth.assertThat(result).isNotNull()
        Truth.assertThat(result).isNotEmpty()

        // Check transaction - reactivate token
        val firstExpectedCallData = YieldSupplyContractCallDataProviderFactory.getReactivateTokenCallData(
            tokenContractAddress = mockedContractAddress,
            maxNetworkFee = BigDecimal.TEN.convertToSdkAmount(cryptoCurrency),
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
        coEvery { walletManager.getYieldContract() } returns yieldContractAddress
        coEvery { walletManager.calculateYieldContract() } returns yieldContractAddress
        coEvery { walletManager.getYieldSupplyStatus(any()) } returns SDKYieldSupplyStatus(
            isActive = true,
            isInitialized = true,
            maxNetworkFee = BigDecimal.TEN,
        )
        coEvery { walletManager.isAllowedToSpend(any()) } returns true

        val result = repository.createEnterTransactions(userWalletId, cryptoCurrencyStatus)

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

        val yieldSupplyStatus = mockk<YieldSupplyStatus>(relaxed = true)

        val result = repository.createExitTransaction(userWalletId, cryptoCurrency, yieldSupplyStatus, null)

        Truth.assertThat(result).isNotNull()
        Truth.assertThat(result.extras).isInstanceOf(EthereumTransactionExtras::class.java)
        Truth.assertThat((result.extras as EthereumTransactionExtras).callData?.data).isEqualTo(expectedCallData.data)
    }
}