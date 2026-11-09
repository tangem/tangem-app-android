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
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.modal.TangemModalActionScope
import com.tangem.core.ui.ds2.modal.TangemModalContentUM
import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.EventMessage
import com.tangem.core.ui.message.ModalMessage
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
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationRequest
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
    private val sentMessages = mutableListOf<EventMessage>()
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
        sentMessages.clear()
        every { messageSender.send(capture(sentMessages)) } returns Unit
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
    fun `GIVEN device key WHEN revoke clicked THEN confirmation names the device and nothing starts`() = runTest {
        // Arrange
        val model = createContentModel()

        // Act
        content(model).onActionClick(DEVICE_REVOKE)
        advanceUntilIdle()

        // Assert
        val modal = sentModal()
        assertThat(modal.content).isEqualTo(
            TangemModalContentUM.Status(
                variant = TangemModalStatus.Variant.Warning,
                title = resourceReference(R.string.prediction_session_key_revoke_confirm_title),
                description = resourceReference(
                    id = R.string.prediction_session_key_revoke_confirm_text,
                    formatArgs = wrappedList(DEVICE_NAME),
                ),
            ),
        )
        assertThat(modal.buttons.map { it.text to it.variant }).containsExactly(
            resourceReference(R.string.prediction_session_key_revoke) to TangemButton.Variant.Primary,
            resourceReference(R.string.common_cancel) to TangemButton.Variant.Secondary,
        ).inOrder()
        verify(exactly = 0) { startOperation(any(), any()) }
    }

    @Test
    fun `GIVEN unknown signer WHEN revoke clicked THEN confirmation names the signer address`() = runTest {
        // Arrange
        val model = createContentModel()

        // Act
        content(model).onActionClick(
            FastTradingKeyActionUM.Revoke(KEY_ADDRESS, FastTradingKeyActionUM.RevokeSubject.UnknownSigner),
        )
        advanceUntilIdle()

        // Assert
        val status = sentModal().content as TangemModalContentUM.Status
        assertThat(status.description).isEqualTo(
            resourceReference(
                id = R.string.prediction_session_key_revoke_confirm_unknown_signer_text,
                formatArgs = wrappedList(KEY_ADDRESS),
            ),
        )
        verify(exactly = 0) { startOperation(any(), any()) }
    }

    @Test
    fun `GIVEN revoke confirmation WHEN confirmed THEN revocation of that key starts and the modal closes`() = runTest {
        // Arrange
        val model = createContentModel()
        content(model).onActionClick(DEVICE_REVOKE)
        advanceUntilIdle()
        var isDismissed = false

        // Act
        sentModal().buttons.first().onClick(TangemModalActionScope { isDismissed = true })
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { startOperation(USER_WALLET_ID, REVOKE) }
        assertThat(isDismissed).isTrue()
        assertThat(sentMessages).hasSize(1)
    }

    @Test
    fun `GIVEN revoke confirmation WHEN cancelled THEN nothing starts`() = runTest {
        // Arrange
        val model = createContentModel()
        content(model).onActionClick(DEVICE_REVOKE)
        advanceUntilIdle()

        // Act
        sentModal().buttons.last().onClick(TangemModalActionScope {})
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { startOperation(any(), any()) }
    }

    @Test
    fun `GIVEN another operation started meanwhile WHEN revoke confirmed THEN busy toast is shown`() = runTest {
        // Arrange
        every { startOperation(USER_WALLET_ID, REVOKE) } returns PolymarketSessionKeyOperationStart.Busy
        val model = createContentModel()
        content(model).onActionClick(DEVICE_REVOKE)
        advanceUntilIdle()

        // Act
        sentModal().buttons.first().onClick(TangemModalActionScope {})
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { startOperation(USER_WALLET_ID, REVOKE) }
        val toast = sentMessages.last() as SnackbarMessage
        assertThat(toast.message).isEqualTo(resourceReference(R.string.prediction_session_key_toast_busy))
    }

    private suspend fun TestScope.createContentModel(): PolymarketFastTradingSettingsModel {
        val model = createModel()
        states.emit(createState(OVERVIEW.right()))
        advanceUntilIdle()
        return model
    }

    private fun sentModal() = (sentMessages.first() as ModalMessage).messageModalUM

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
        val DEVICE_NAME = stringReference("Galaxy S24")
        val DEVICE_REVOKE = FastTradingKeyActionUM.Revoke(
            address = KEY_ADDRESS,
            subject = FastTradingKeyActionUM.RevokeSubject.Device(DEVICE_NAME),
        )
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

        fun createState(overview: Either<PolymarketWalletError, PolymarketSessionKeyOverview>) =
            PolymarketFastTradingState(overview = overview, operation = null, finished = null)
    }
}