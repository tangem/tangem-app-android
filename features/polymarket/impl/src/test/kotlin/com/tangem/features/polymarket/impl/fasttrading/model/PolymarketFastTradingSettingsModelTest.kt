package com.tangem.features.polymarket.impl.fasttrading.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.res.R
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.interactor.PolymarketSessionKeyOverview
import com.tangem.domain.polymarket.model.PolymarketFastTradingState
import com.tangem.domain.polymarket.model.PolymarketWalletError
import com.tangem.domain.polymarket.model.PolymarketWalletState
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.domain.polymarket.sessionkey.PolymarketDeviceIdProvider
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyAssessment
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyDeviceState
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyReconciliation
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperation
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationRequest
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationStage
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationStart
import com.tangem.domain.polymarket.usecase.ObservePolymarketFastTradingUseCase
import com.tangem.domain.polymarket.usecase.RefreshPolymarketFastTradingUseCase
import com.tangem.domain.polymarket.usecase.StartPolymarketSessionKeyOperationUseCase
import com.tangem.features.polymarket.impl.fasttrading.PolymarketFastTradingSettingsComponent
import com.tangem.features.polymarket.impl.fasttrading.ui.state.FastTradingKeyActionUM
import com.tangem.features.polymarket.impl.fasttrading.ui.state.PolymarketFastTradingSettingsUM
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketFastTradingSettingsModelTest {

    private val router: Router = mockk(relaxed = true)
    private val observeFastTrading: ObservePolymarketFastTradingUseCase = mockk()
    private val refreshFastTrading: RefreshPolymarketFastTradingUseCase = mockk()
    private val deviceIdProvider: PolymarketDeviceIdProvider = mockk()
    private val startOperation: StartPolymarketSessionKeyOperationUseCase = mockk()
    private val messageSender: UiMessageSender = mockk()
    private val states = MutableSharedFlow<PolymarketFastTradingState>(replay = 1)
    private var model: PolymarketFastTradingSettingsModel? = null

    @BeforeEach
    fun setup() {
        clearMocks(router, observeFastTrading, refreshFastTrading, deviceIdProvider, startOperation, messageSender)
        states.resetReplayCache()
        every { observeFastTrading(USER_WALLET_ID) } returns states
        every { deviceIdProvider.deviceName } returns "Pixel 9"
        coEvery { refreshFastTrading(USER_WALLET_ID) } returns Unit
        every { startOperation(USER_WALLET_ID, REVOKE) } returns PolymarketSessionKeyOperationStart.Started(OPERATION_ID)
        every { messageSender.send(any()) } returns Unit
    }

    @AfterEach
    fun destroyModel() {
        model?.onDestroy()
    }

    @Test
    fun `GIVEN no state yet WHEN model is created THEN state is loading`() = runTest {
        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(PolymarketFastTradingSettingsUM.Loading::class.java)
    }

    @Test
    fun `GIVEN overview fails WHEN retry clicked THEN refresh is requested`() = runTest {
        // Arrange
        val model = createModel()
        states.emit(createState(PolymarketWalletError.Network.left()))
        advanceUntilIdle()

        // Act
        val error = model.uiState.value as PolymarketFastTradingSettingsUM.Error
        error.onRetryClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { refreshFastTrading(USER_WALLET_ID) }
    }

    @Test
    fun `GIVEN content WHEN pull to refresh THEN refreshing until refresh completes`() = runTest {
        // Arrange
        val refreshed = CompletableDeferred<Unit>()
        coEvery { refreshFastTrading(USER_WALLET_ID) } coAnswers { refreshed.await() }
        val model = createModel()
        states.emit(createState(OVERVIEW.right()))
        advanceUntilIdle()

        // Act
        content(model).pullToRefreshConfig.onRefresh(PullToRefreshConfig.ShowRefreshState())
        advanceUntilIdle()
        val whileRefreshing = content(model).pullToRefreshConfig.isRefreshing
        refreshed.complete(Unit)
        advanceUntilIdle()

        // Assert
        assertThat(whileRefreshing).isTrue()
        assertThat(content(model).pullToRefreshConfig.isRefreshing).isFalse()
        coVerify(exactly = 1) { refreshFastTrading(USER_WALLET_ID) }
    }

    @Test
    fun `GIVEN content WHEN back clicked THEN route is popped`() = runTest {
        // Arrange
        val model = createModel()
        states.emit(createState(OVERVIEW.right()))
        advanceUntilIdle()

        // Act
        content(model).onBackClick()

        // Assert
        verify(exactly = 1) { router.pop(any()) }
    }

    @Test
    fun `GIVEN content WHEN revoke clicked THEN revocation of that key is started`() = runTest {
        // Arrange
        val model = createModel()
        states.emit(createState(OVERVIEW.right()))
        advanceUntilIdle()

        // Act
        content(model).onActionClick(FastTradingKeyActionUM.Revoke(KEY_ADDRESS))
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { startOperation(USER_WALLET_ID, REVOKE) }
        verify(exactly = 0) { messageSender.send(any()) }
    }

    @Test
    fun `GIVEN another operation running WHEN revoke clicked THEN busy toast is shown`() = runTest {
        // Arrange
        every { startOperation(USER_WALLET_ID, REVOKE) } returns PolymarketSessionKeyOperationStart.Busy
        val message = slot<SnackbarMessage>()
        every { messageSender.send(capture(message)) } returns Unit
        val model = createModel()
        states.emit(createState(OVERVIEW.right()))
        advanceUntilIdle()

        // Act
        content(model).onActionClick(FastTradingKeyActionUM.Revoke(KEY_ADDRESS))
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { messageSender.send(any()) }
        assertThat(message.captured.message).isEqualTo(resourceReference(R.string.prediction_session_key_toast_busy))
    }

    @Test
    fun `GIVEN started revocation WHEN it is submitted THEN revoking toast is shown once`() = runTest {
        // Arrange
        val message = slot<SnackbarMessage>()
        every { messageSender.send(capture(message)) } returns Unit
        val model = createModel()
        states.emit(createState(OVERVIEW.right()))
        advanceUntilIdle()
        content(model).onActionClick(FastTradingKeyActionUM.Revoke(KEY_ADDRESS))

        // Act
        states.emit(createState(OVERVIEW.right(), revocation(PolymarketSessionKeyOperationStage.SIGNING)))
        advanceUntilIdle()
        val sentWhileSigning = message.isCaptured
        states.emit(createState(OVERVIEW.right(), revocation(PolymarketSessionKeyOperationStage.REVOKING)))
        advanceUntilIdle()
        states.emit(createState(OVERVIEW.right(), revocation(PolymarketSessionKeyOperationStage.REVOKED)))
        advanceUntilIdle()

        // Assert
        assertThat(sentWhileSigning).isFalse()
        verify(exactly = 1) { messageSender.send(any()) }
        assertThat(message.captured.message).isEqualTo(resourceReference(R.string.prediction_session_key_toast_revoking))
    }

    @Test
    fun `GIVEN started revocation WHEN it ends at signing THEN no revoking toast is shown`() = runTest {
        // Arrange
        val model = createModel()
        states.emit(createState(OVERVIEW.right()))
        advanceUntilIdle()
        content(model).onActionClick(FastTradingKeyActionUM.Revoke(KEY_ADDRESS))

        // Act
        states.emit(createState(OVERVIEW.right(), revocation(PolymarketSessionKeyOperationStage.SIGNING)))
        advanceUntilIdle()
        states.emit(createState(OVERVIEW.right()))
        advanceUntilIdle()
        states.emit(createState(OVERVIEW.right(), revocation(PolymarketSessionKeyOperationStage.REVOKING)))
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { messageSender.send(any()) }
    }

    @Test
    fun `GIVEN started revocation WHEN another operation is submitted THEN no revoking toast is shown`() = runTest {
        // Arrange
        val model = createModel()
        states.emit(createState(OVERVIEW.right()))
        advanceUntilIdle()
        content(model).onActionClick(FastTradingKeyActionUM.Revoke(KEY_ADDRESS))

        // Act
        states.emit(
            createState(
                OVERVIEW.right(),
                revocation(PolymarketSessionKeyOperationStage.REVOKING).copy(id = OPERATION_ID + 1),
            ),
        )
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { messageSender.send(any()) }
    }

    private fun content(model: PolymarketFastTradingSettingsModel) =
        model.uiState.value as PolymarketFastTradingSettingsUM.Content

    private fun TestScope.createModel() = PolymarketFastTradingSettingsModel(
        dispatchers = createTestingCoroutineDispatcherProvider(),
        paramsContainer = MutableParamsContainer(PolymarketFastTradingSettingsComponent.Params(USER_WALLET_ID)),
        router = router,
        observeFastTrading = observeFastTrading,
        refreshFastTrading = refreshFastTrading,
        startOperation = startOperation,
        messageSender = messageSender,
        deviceIdProvider = deviceIdProvider,
    ).also { model = it }

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        const val KEY_ADDRESS = "0x5e55000000000000000000000000000000000002"
        const val OPERATION_ID = 5L
        val REVOKE = PolymarketSessionKeyOperationRequest.Revoke(KEY_ADDRESS)
        val OVERVIEW = PolymarketSessionKeyOverview(
            wallet = PolymarketWalletState(
                depositWalletAddress = "0x2222222222222222222222222222222222222222",
                status = PolymarketWalletStatus.READY_TO_TRADE,
            ),
            localKeyAddress = null,
            assessment = PolymarketSessionKeyAssessment(
                deviceState = PolymarketSessionKeyDeviceState.OfferEnable,
                reconciliation = PolymarketSessionKeyReconciliation.Unavailable,
                nextChangeAt = null,
            ),
            isTradable = false,
        )

        fun createState(
            overview: Either<PolymarketWalletError, PolymarketSessionKeyOverview>,
            operation: PolymarketSessionKeyOperation? = null,
        ) = PolymarketFastTradingState(overview = overview, operation = operation, finished = null)

        fun revocation(stage: PolymarketSessionKeyOperationStage) = PolymarketSessionKeyOperation(
            id = OPERATION_ID,
            request = REVOKE,
            targetAddress = KEY_ADDRESS,
            stage = stage,
        )
    }
}