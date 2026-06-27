package com.tangem.features.yield.supply.impl.subcomponents

import arrow.core.right
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.datasource.local.appsflyer.AppsFlyerStore
import com.tangem.domain.account.status.usecase.GetFeePaidCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.usecase.YieldSupplyPendingTracker
import com.tangem.features.yield.supply.impl.common.YieldSupplyAlertFactory
import com.tangem.features.yield.supply.impl.subcomponents.notifications.YieldSupplyNotificationsUpdateTrigger
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.BeforeEach
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Shared fixtures, mocks and builders for the Yield Supply transactional model tests
 * (Approve / StopEarning / StartEarning). Subclasses declare their own unique mocks and build
 * the concrete model via the base mocks; tests read [uiState] synchronously thanks to the
 * Unconfined [TestingCoroutineDispatcherProvider].
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal abstract class YieldSupplyActionModelTestBase {

    protected val analytics: AnalyticsEventHandler = mockk(relaxed = true)
    protected val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk()
    protected val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase = mockk()
    protected val sendTransactionUseCase: SendTransactionUseCase = mockk()
    protected val getFeeUseCase: GetFeeUseCase = mockk()
    protected val urlOpener: UrlOpener = mockk(relaxed = true)
    protected val notificationsUpdateTrigger: YieldSupplyNotificationsUpdateTrigger = mockk(relaxed = true)
    protected val alertFactory: YieldSupplyAlertFactory = mockk(relaxed = true)
    protected val pendingTracker: YieldSupplyPendingTracker = mockk(relaxed = true)
    protected val yieldSupplyRepository: YieldSupplyRepository = mockk(relaxed = true)
    protected val appsFlyerStore: AppsFlyerStore = mockk(relaxed = true)

    protected val userWalletId = UserWalletId("abcdef012345")
    protected val userWallet: UserWallet = mockk(relaxed = true) {
        every { walletId } returns userWalletId
    }

    protected val token: CryptoCurrency.Token = token()
    protected val coin: CryptoCurrency.Coin = coin()
    protected val cryptoCurrencyStatus: CryptoCurrencyStatus = statusOf(token)
    protected val cryptoCurrencyStatusFlow = MutableStateFlow(cryptoCurrencyStatus)

    @BeforeEach
    fun baseSetUp() {
        coEvery { getSelectedAppCurrencyUseCase.invokeSync() } returns AppCurrency.Default.right()
        every { notificationsUpdateTrigger.hasErrorFlow } returns MutableStateFlow(false)
        coEvery { getFeePaidCryptoCurrencyStatusSyncUseCase(any(), any()) } returns cryptoCurrencyStatus.right()
    }

    /** A [StandardTestDispatcher] for every role so `advanceUntilIdle()` drives the model's coroutines. */
    protected fun TestScope.createTestingCoroutineDispatcherProvider(): TestingCoroutineDispatcherProvider {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        return TestingCoroutineDispatcherProvider(
            main = testDispatcher,
            mainImmediate = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
            single = testDispatcher,
        )
    }

    /** Network fee is paid in the native coin (token amounts are rejected by `increaseGasLimitBy`). */
    protected fun coinAmount(value: BigDecimal): Amount =
        Amount(currencySymbol = "ETH", value = value, decimals = 18, type = AmountType.Coin)

    protected fun ethFee(value: BigDecimal = BigDecimal("0.001")): Fee.Ethereum.EIP1559 = Fee.Ethereum.EIP1559(
        maxFeePerGas = BigInteger.valueOf(1_000_000_000L),
        priorityFee = BigInteger.ONE,
        gasLimit = BigInteger.valueOf(21_000),
        amount = coinAmount(value),
    )

    protected fun transactionFee(value: BigDecimal = BigDecimal("0.001")): TransactionFee.Single =
        TransactionFee.Single(normal = ethFee(value))

    protected fun uncompiledTx(fee: Fee = ethFee()): TransactionData.Uncompiled = TransactionData.Uncompiled(
        fee = fee,
        amount = coinAmount(BigDecimal.ONE),
        contractAddress = null,
        sourceAddress = SOURCE_ADDRESS,
        destinationAddress = DESTINATION_ADDRESS,
        extras = null,
    )

    protected fun statusOf(currency: CryptoCurrency): CryptoCurrencyStatus = CryptoCurrencyStatus(
        currency = currency,
        value = CryptoCurrencyStatus.Custom(
            amount = BigDecimal.TEN,
            fiatAmount = BigDecimal.TEN,
            fiatRate = BigDecimal.ONE,
            priceChange = BigDecimal.ZERO,
            stakingBalance = null,
            yieldSupplyStatus = YieldSupplyStatus(
                isActive = true,
                isInitialized = true,
                isAllowedToSpend = true,
                effectiveProtocolBalance = BigDecimal.ONE,
            ),
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            networkAddress = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(
                    value = SOURCE_ADDRESS,
                    type = NetworkAddress.Address.Type.Primary,
                ),
            ),
            sources = CryptoCurrencyStatus.Sources(),
        ),
    )

    protected fun token(): CryptoCurrency.Token = CryptoCurrency.Token(
        id = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId("ethereum"),
            suffix = CryptoCurrency.ID.Suffix.RawID("ethereum"),
        ),
        network = network(),
        name = "TEST_TOKEN",
        symbol = "TTK",
        decimals = 6,
        iconUrl = null,
        isCustom = false,
        contractAddress = "0xToken",
    )

    protected fun coin(): CryptoCurrency.Coin = CryptoCurrency.Coin(
        id = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.COIN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId("ethereum"),
            suffix = CryptoCurrency.ID.Suffix.RawID("ethereum"),
        ),
        network = network(),
        name = "TEST_COIN",
        symbol = "ETH",
        decimals = 18,
        iconUrl = null,
        isCustom = false,
    )

    protected fun network(): Network {
        val derivationPath = Network.DerivationPath.None
        return Network(
            id = Network.ID(value = "ethereum", derivationPath = derivationPath),
            name = "Ethereum",
            currencySymbol = "ETH",
            derivationPath = derivationPath,
            isTestnet = false,
            standardType = Network.StandardType.Unspecified("UNSPECIFIED"),
            hasFiatFeeRate = true,
            canHandleTokens = true,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = Network.NameResolvingType.NONE,
        )
    }

    protected companion object {
        const val SOURCE_ADDRESS = "0x1111111111111111111111111111111111111111"
        const val DESTINATION_ADDRESS = "0x2222222222222222222222222222222222222222"
    }
}