package com.tangem.features.tangempay.multichain.choosenetwork

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.extensions.iconResId
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.PaymentNetworkStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.usecase.CreatePaymentNetworkContractUseCase
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

internal class PaymentChooseNetworkModelTest {

    private val supplier: PaymentAccountStatusSupplier = mockk()
    private val createContractUseCase: CreatePaymentNetworkContractUseCase = mockk()
    private val listener: ChooseNetworkListener = mockk(relaxed = true)

    private val statusFlow = MutableSharedFlow<AccountStatus.Payment>(replay = 1)

    // Built in setUp, not in field initializers: the network fixture stubs the Network.iconResId
    // extension, which records correctly only while mockkStatic(NetworkIconExtKt) is active.
    private lateinit var polygon: Network
    private lateinit var notIssued: PaymentNetworkStatus.NotIssued

    @BeforeEach
    fun setUp() {
        mockkStatic("com.tangem.common.ui.extensions.NetworkIconExtKt")
        polygon = network(networkName = "Polygon", networkRawId = "polygon")
        notIssued = PaymentNetworkStatus.NotIssued(
            network = polygon,
            cryptoCurrencies = listOf(currency("USDC")),
        )
        every { supplier.invoke(WALLET_ID) } returns statusFlow
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `GIVEN NotIssued row tapped WHEN contract creation is in flight THEN row shows Loading and reentry is guarded`() =
        runTest {
            // Arrange
            coEvery { createContractUseCase(WALLET_ID, polygon) } coAnswers { awaitCancellation() }
            val model = createModel(testScope = this)
            statusFlow.emit(paymentStatus(listOf(notIssued)))
            advanceUntilIdle()

            // Act — tap twice: the second tap must be swallowed by the Loading guard
            model.uiState.value.fastWay.single().onClick()
            advanceUntilIdle()
            model.uiState.value.fastWay.single().onClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.fastWay.single().state).isEqualTo(PaymentNetworkItemUM.State.Loading)
            coVerify(exactly = 1) { createContractUseCase(WALLET_ID, polygon) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN contract creation fails WHEN NotIssued row tapped THEN row shows Error and retry re-invokes use case`() =
        runTest {
            // Arrange
            coEvery { createContractUseCase(WALLET_ID, polygon) } returns VisaApiError.Unspecified.left()
            val model = createModel(testScope = this)
            statusFlow.emit(paymentStatus(listOf(notIssued)))
            advanceUntilIdle()

            // Act
            model.uiState.value.fastWay.single().onClick()
            advanceUntilIdle()

            // Assert — Error with a retry hook
            val errorRow = model.uiState.value.fastWay.single()
            assertThat(errorRow.state).isEqualTo(PaymentNetworkItemUM.State.Error)
            assertThat(errorRow.onRetry).isNotNull()

            // Act — retry starts a second attempt
            errorRow.onRetry?.invoke()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 2) { createContractUseCase(WALLET_ID, polygon) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN row is Loading WHEN network becomes Available THEN override cleared and Receive auto-opens`() =
        runTest {
            // Arrange
            coEvery { createContractUseCase(WALLET_ID, polygon) } returns Unit.right()
            val model = createModel(testScope = this)
            statusFlow.emit(paymentStatus(listOf(notIssued)))
            advanceUntilIdle()
            model.uiState.value.fastWay.single().onClick()

            // Act — the poller's refresh lands: the network flips to Available
            val available = PaymentNetworkStatus.Available(
                network = polygon,
                depositAddress = "0xDEPOSIT",
                cryptoCurrencyStatuses = listOf(CryptoCurrencyStatus(currency = currency("USDC"), value = mockk())),
            )
            statusFlow.emit(paymentStatus(listOf(available)))
            advanceUntilIdle()

            // Assert — success signal honored: override gone, Receive opened for the same network
            assertThat(model.uiState.value.fastWay.single().state).isEqualTo(PaymentNetworkItemUM.State.Idle)
            verify(exactly = 1) { listener.onSelectAvailable(networkRawId = "polygon") }
            model.onDestroy()
        }

    @Test
    fun `GIVEN contract created WHEN network never flips to Available THEN row shows Error after pending timeout`() =
        runTest {
            // Arrange
            coEvery { createContractUseCase(WALLET_ID, polygon) } returns Unit.right()
            val model = createModel(testScope = this)
            statusFlow.emit(paymentStatus(listOf(notIssued)))
            advanceUntilIdle()

            // Act
            model.uiState.value.fastWay.single().onClick()
            advanceTimeBy(91.seconds)

            // Assert — stuck spinner is converted to a retryable Error
            val row = model.uiState.value.fastWay.single()
            assertThat(row.state).isEqualTo(PaymentNetworkItemUM.State.Error)
            assertThat(row.onRetry).isNotNull()
            verify(exactly = 0) { listener.onSelectAvailable(networkRawId = any()) }
            model.onDestroy()
        }

    private fun createModel(testScope: TestScope): PaymentChooseNetworkModel {
        return PaymentChooseNetworkModel(
            paramsContainer = MutableParamsContainer(
                PaymentChooseNetworkComponent.Params(walletId = WALLET_ID, listener = listener),
            ),
            paymentAccountStatusSupplier = supplier,
            createPaymentNetworkContractUseCase = createContractUseCase,
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

    private fun network(networkName: String, networkRawId: String): Network {
        val network: Network = mockk {
            every { name } returns networkName
            every { rawId } returns networkRawId
        }
        every { network.iconResId } returns ICON_RES_ID
        return network
    }

    private fun currency(symbol: String): CryptoCurrency.Token {
        val token: CryptoCurrency.Token = mockk()
        every { token.symbol } returns symbol
        return token
    }

    private companion object {
        val WALLET_ID = UserWalletId("1234567890ABCDEF")
        const val ICON_RES_ID = 42
    }
}