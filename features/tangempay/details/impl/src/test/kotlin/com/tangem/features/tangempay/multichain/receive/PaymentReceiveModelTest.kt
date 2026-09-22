package com.tangem.features.tangempay.multichain.receive

import com.tangem.common.ui.extensions.networkIconResId
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.PaymentNetworkStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class PaymentReceiveModelTest {

    private val supplier: PaymentAccountStatusSupplier = mockk()
    private val clipboardManager: ClipboardManager = mockk(relaxed = true)
    private val shareManager: ShareManager = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)

    private val statusFlow = MutableSharedFlow<AccountStatus.Payment>(replay = 1)

    @BeforeEach
    fun setUp() {
        mockkStatic("com.tangem.common.ui.extensions.NetworkIconExtKt")
        every { supplier.invoke(WALLET_ID) } returns statusFlow
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `GIVEN selected network resolved WHEN status arrives THEN address popup showed carries network and chain id`() =
        runTest {
            // Arrange
            val model = createModel(testScope = this)

            // Act
            statusFlow.emit(paymentStatus(listOf(available(networkName = "Polygon", chainId = 137L))))
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) {
                analytics.send(
                    TangemPayAnalyticsEvents.Multichain.FastWayNetworkAddressPopupShowed(
                        blockchain = "Polygon",
                        chainId = 137L,
                    ),
                )
            }
            model.onDestroy()
        }

    @Test
    fun `GIVEN sheet already shown WHEN further statuses arrive THEN address popup showed is not sent again`() =
        runTest {
            // Arrange
            val model = createModel(testScope = this)
            statusFlow.emit(paymentStatus(listOf(available(networkName = "Polygon", chainId = 137L))))
            advanceUntilIdle()

            // Act — a balance refresh of a sheet already on screen
            statusFlow.emit(paymentStatus(listOf(available(networkName = "Polygon", chainId = 137L))))
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) {
                analytics.send(
                    TangemPayAnalyticsEvents.Multichain.FastWayNetworkAddressPopupShowed(
                        blockchain = "Polygon",
                        chainId = 137L,
                    ),
                )
            }
            model.onDestroy()
        }

    @Test
    fun `GIVEN status without the selected network WHEN it arrives THEN nothing is reported`() = runTest {
        // Arrange
        val model = createModel(testScope = this)

        // Act
        statusFlow.emit(paymentStatus(emptyList()))
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { analytics.send(any()) }
        model.onDestroy()
    }

    private fun createModel(testScope: TestScope): PaymentReceiveModel {
        return PaymentReceiveModel(
            paramsContainer = MutableParamsContainer(
                PaymentReceiveComponent.Params(
                    walletId = WALLET_ID,
                    networkRawId = NETWORK_RAW_ID,
                    onDismiss = {},
                ),
            ),
            paymentAccountStatusSupplier = supplier,
            clipboardManager = clipboardManager,
            shareManager = shareManager,
            uiMessageSender = uiMessageSender,
            analytics = analytics,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        )
    }

    private fun TestScope.createTestingCoroutineDispatcherProvider(): TestingCoroutineDispatcherProvider {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        return TestingCoroutineDispatcherProvider(
            main = testDispatcher,
            mainImmediate = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
            single = testDispatcher,
        )
    }

    private fun paymentStatus(networks: List<PaymentNetworkStatus>): AccountStatus.Payment {
        val loaded: PaymentAccountStatusValue.Loaded = mockk {
            every { this@mockk.networks } returns networks
        }
        return mockk { every { value } returns loaded }
    }

    private fun available(networkName: String, chainId: Long): PaymentNetworkStatus.Available {
        val network: Network = mockk {
            every { name } returns networkName
            every { rawId } returns NETWORK_RAW_ID
            every { isTestnet } returns false
        }
        return PaymentNetworkStatus.Available(
            network = network,
            chainId = chainId,
            depositAddress = "0xDEPOSIT",
            cryptoCurrencyStatuses = listOf(
                CryptoCurrencyStatus(currency = token(symbol = "USDC", network = network), value = mockk()),
            ),
        )
    }

    private fun token(symbol: String, network: Network): CryptoCurrency.Token {
        val currency: CryptoCurrency.Token = mockk()
        every { currency.symbol } returns symbol
        every { currency.network } returns network
        every { currency.iconUrl } returns null
        every { currency.isCustom } returns false
        every { currency.contractAddress } returns "0xCONTRACT"
        every { currency.id } returns mockk { every { rawCurrencyId } returns CryptoCurrency.RawID(symbol) }
        every { currency.networkIconResId } returns ICON_RES_ID
        return currency
    }

    private companion object {
        val WALLET_ID = UserWalletId("1234567890ABCDEF")
        const val NETWORK_RAW_ID = "polygon"
        const val ICON_RES_ID = 1
    }
}