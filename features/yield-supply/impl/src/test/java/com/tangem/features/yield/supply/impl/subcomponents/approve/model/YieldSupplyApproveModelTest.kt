package com.tangem.features.yield.supply.impl.subcomponents.approve.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.TangemBlogUrlBuilder
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.usecase.CreateApprovalTransactionUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetContractAddressUseCase
import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyFeeUM
import com.tangem.features.yield.supply.impl.subcomponents.YieldSupplyActionModelTestBase
import com.tangem.features.yield.supply.impl.subcomponents.approve.YieldSupplyApproveComponent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class YieldSupplyApproveModelTest : YieldSupplyActionModelTestBase() {

    private val createApprovalTransactionUseCase: CreateApprovalTransactionUseCase = mockk()
    private val getContractAddressUseCase: YieldSupplyGetContractAddressUseCase = mockk()
    private val callback: YieldSupplyApproveComponent.ModelCallback = mockk(relaxed = true)

    @BeforeEach
    fun setUp() {
        coEvery { getContractAddressUseCase(any(), any()) } returns "0xSpender".right()
        coEvery {
            createApprovalTransactionUseCase(any(), any(), any(), any(), any())
        } returns uncompiledTx().right()
        coEvery { getFeeUseCase(any(), any(), any()) } returns transactionFee().right()
        coEvery { sendTransactionUseCase(txData = any(), userWallet = any(), network = any()) } returns "0xhash".right()
    }

    @Test
    fun `GIVEN successful fee load WHEN model created THEN fee content and button enabled`() = runTest {
        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.yieldSupplyFeeUM).isInstanceOf(YieldSupplyFeeUM.Content::class.java)
        assertThat(model.uiState.value.isPrimaryButtonEnabled).isTrue()
        coVerify { notificationsUpdateTrigger.triggerUpdate(any()) }
    }

    @Test
    fun `GIVEN get fee fails WHEN model created THEN fee error state`() = runTest {
        // Arrange
        coEvery { getFeeUseCase(any(), any(), any()) } returns GetFeeError.UnknownError.left()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.yieldSupplyFeeUM).isEqualTo(YieldSupplyFeeUM.Error)
    }

    @Test
    fun `GIVEN non-token currency WHEN model created THEN fee not loaded`() = runTest {
        // Act
        val model = createModel(statusFlow = MutableStateFlow(statusOf(coin)))
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.yieldSupplyFeeUM).isEqualTo(YieldSupplyFeeUM.Loading)
        coVerify(exactly = 0) { getFeeUseCase(any(), any(), any()) }
    }

    @Test
    fun `GIVEN contract address missing WHEN model created THEN fee not loaded`() = runTest {
        // Arrange
        coEvery { getContractAddressUseCase(any(), any()) } returns (null as String?).right()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.yieldSupplyFeeUM).isEqualTo(YieldSupplyFeeUM.Loading)
        coVerify(exactly = 0) { getFeeUseCase(any(), any(), any()) }
    }

    @Test
    fun `GIVEN content loaded WHEN onClick THEN sends transaction tracks pending and notifies sent`() = runTest {
        // Arrange
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.onClick()
        advanceUntilIdle()

        // Assert
        verify { callback.onTransactionProgress(true) }
        coVerify { pendingTracker.addPending(userWalletId, any(), any()) }
        verify { callback.onTransactionSent() }

        // Token fee asset (default fee currency is the token itself)
        val events = mutableListOf<AnalyticsEvent>()
        verify { analytics.send(capture(events)) }
        val sent = events.filterIsInstance<Basic.TransactionSent>().single()
        assertThat(sent.params["Fee Token"]).isEqualTo("TTK")
        assertThat(sent.params["Fee Asset Type"]).isEqualTo(AnalyticsParam.FeeAssetType.Token.value)
    }

    @Test
    fun `GIVEN coin fee currency WHEN onClick succeeds THEN transaction sent analytics carries coin fee asset`() = runTest {
        // Arrange — network fee paid in the native coin, not the token
        coEvery { getFeePaidCryptoCurrencyStatusSyncUseCase(any(), any()) } returns statusOf(coin).right()
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.onClick()
        advanceUntilIdle()

        // Assert
        val events = mutableListOf<AnalyticsEvent>()
        verify { analytics.send(capture(events)) }
        val sent = events.filterIsInstance<Basic.TransactionSent>().single()
        assertThat(sent.params["Fee Token"]).isEqualTo("ETH")
        assertThat(sent.params["Fee Asset Type"]).isEqualTo(AnalyticsParam.FeeAssetType.Coin.value)
    }

    @Test
    fun `GIVEN fee not loaded WHEN onClick THEN does not send transaction`() = runTest {
        // Arrange — fee load fails so the fee state is Error; onClick reports progress then early-returns
        coEvery { getFeeUseCase(any(), any(), any()) } returns GetFeeError.UnknownError.left()
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.onClick()
        advanceUntilIdle()

        // Assert
        verify { callback.onTransactionProgress(true) }
        coVerify(exactly = 0) { sendTransactionUseCase(txData = any(), userWallet = any(), network = any()) }
    }

    @Test
    fun `GIVEN notifications report an error WHEN flag emitted THEN primary button disabled`() = runTest {
        // Arrange
        val hasErrorFlow = MutableStateFlow(false)
        every { notificationsUpdateTrigger.hasErrorFlow } returns hasErrorFlow
        val model = createModel()
        advanceUntilIdle()
        assertThat(model.uiState.value.isPrimaryButtonEnabled).isTrue()

        // Act
        hasErrorFlow.value = true
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.isPrimaryButtonEnabled).isFalse()
    }

    @Test
    fun `GIVEN content loaded WHEN onClick and send fails THEN shows error and stops progress`() = runTest {
        // Arrange
        coEvery {
            sendTransactionUseCase(txData = any(), userWallet = any(), network = any())
        } returns SendTransactionError.UnknownError().left()
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.onClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.isTransactionSending).isFalse()
        verify { alertFactory.getSendTransactionErrorState(any(), any(), any()) }
        verify { callback.onTransactionProgress(false) }
        verify(exactly = 0) { callback.onTransactionSent() }
    }

    @Test
    fun `WHEN onReadMoreClick THEN opens url`() = runTest {
        // Arrange — TangemBlogUrlBuilder.build is a real suspend object; stub it to isolate the model's intent
        mockkObject(TangemBlogUrlBuilder)
        try {
            coEvery { TangemBlogUrlBuilder.build(any()) } returns BLOG_URL
            val model = createModel()
            advanceUntilIdle()

            // Act
            model.onReadMoreClick()
            advanceUntilIdle()

            // Assert
            verify { urlOpener.openUrl(BLOG_URL) }
        } finally {
            unmockkObject(TangemBlogUrlBuilder)
        }
    }

    private fun TestScope.createModel(
        statusFlow: StateFlow<CryptoCurrencyStatus> = cryptoCurrencyStatusFlow,
    ): YieldSupplyApproveModel = YieldSupplyApproveModel(
        dispatchers = createTestingCoroutineDispatcherProvider(),
        paramsContainer = MutableParamsContainer(
            YieldSupplyApproveComponent.Params(
                userWallet = userWallet,
                cryptoCurrencyStatusFlow = statusFlow,
                callback = callback,
            ),
        ),
        analyticsEventHandler = analytics,
        urlOpener = urlOpener,
        yieldSupplyNotificationsUpdateTrigger = notificationsUpdateTrigger,
        createApprovalTransactionUseCase = createApprovalTransactionUseCase,
        getFeeUseCase = getFeeUseCase,
        sendTransactionUseCase = sendTransactionUseCase,
        getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
        getFeePaidCryptoCurrencyStatusSyncUseCase = getFeePaidCryptoCurrencyStatusSyncUseCase,
        yieldSupplyGetContractAddressUseCase = getContractAddressUseCase,
        yieldSupplyPendingTracker = pendingTracker,
        yieldSupplyAlertFactory = alertFactory,
    )

    private companion object {
        const val BLOG_URL = "https://tangem.com/blog"
    }
}