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
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.EventMessage
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.interactor.PolymarketSessionKeyOverview
import com.tangem.domain.polymarket.model.PolymarketFastTradingState
import com.tangem.domain.polymarket.model.PolymarketSessionKey
import com.tangem.domain.polymarket.model.PolymarketSessionKeyStatus
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
import com.tangem.domain.wallets.models.errors.GetUserWalletError
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.polymarket.impl.fasttrading.PolymarketFastTradingSettingsComponent
import com.tangem.features.polymarket.impl.fasttrading.ui.state.FastTradingKeyActionUM
import com.tangem.features.polymarket.impl.fasttrading.ui.state.PolymarketFastTradingSettingsUM
import com.tangem.test.core.ProvideTestModels
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketFastTradingSettingsModelTest {

    private val router: Router = mockk(relaxed = true)
    private val observeFastTrading: ObservePolymarketFastTradingUseCase = mockk()
    private val refreshFastTrading: RefreshPolymarketFastTradingUseCase = mockk()
    private val deviceIdProvider: PolymarketDeviceIdProvider = mockk()
    private val startOperation: StartPolymarketSessionKeyOperationUseCase = mockk()
    private val messageSender: UiMessageSender = mockk()
    private val getUserWallet: GetUserWalletUseCase = mockk()
    private val sentMessages = mutableListOf<EventMessage>()
    private val states = MutableSharedFlow<PolymarketFastTradingState>(replay = 1)
    private var model: PolymarketFastTradingSettingsModel? = null

    @BeforeEach
    fun setup() {
        clearMocks(router, observeFastTrading, refreshFastTrading, deviceIdProvider, startOperation, messageSender, getUserWallet)
        states.resetReplayCache()
        every { observeFastTrading(USER_WALLET_ID) } returns states
        every { deviceIdProvider.deviceName } returns "Pixel 9"
        coEvery { refreshFastTrading(USER_WALLET_ID) } returns Unit
        every { startOperation(USER_WALLET_ID, REVOKE) } returns PolymarketSessionKeyOperationStart.Started(OPERATION_ID)
        every { getUserWallet(USER_WALLET_ID) } returns HOT_WALLET.right()
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
    fun `GIVEN hot wallet and device key WHEN revoke clicked THEN confirmation names the device and nothing starts`() =
        runTest {
            // Arrange
            val model = createContentModel()

            // Act
            content(model).onActionClick(DEVICE_REVOKE)
            advanceUntilIdle()

            // Assert
            val confirmation = content(model).revokeConfirmation
            assertThat(confirmation?.description).isEqualTo(
                resourceReference(
                    id = R.string.prediction_session_key_revoke_confirm_text,
                    formatArgs = wrappedList(DEVICE_NAME),
                ),
            )
            verify(exactly = 0) { startOperation(any(), any()) }
        }

    @Test
    fun `GIVEN unknown signer WHEN revoke clicked THEN confirmation names the signer address`() = runTest {
        // Arrange
        val model = createContentModel()

        // Act
        content(model).onActionClick(UNKNOWN_SIGNER_REVOKE)
        advanceUntilIdle()

        // Assert
        assertThat(content(model).revokeConfirmation?.description).isEqualTo(
            resourceReference(
                id = R.string.prediction_session_key_revoke_confirm_unknown_signer_text,
                formatArgs = wrappedList(KEY_ADDRESS),
            ),
        )
        verify(exactly = 0) { startOperation(any(), any()) }
    }

    @Test
    fun `GIVEN revoke confirmation WHEN confirmed THEN revocation of that key starts and confirmation clears`() =
        runTest {
            // Arrange
            val model = createContentModel()
            content(model).onActionClick(DEVICE_REVOKE)
            advanceUntilIdle()

            // Act
            content(model).revokeConfirmation?.onConfirm?.invoke()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { startOperation(USER_WALLET_ID, REVOKE) }
            assertThat(content(model).revokeConfirmation).isNull()
            assertThat(sentMessages).isEmpty()
        }

    @Test
    fun `GIVEN revoke confirmation WHEN dismissed THEN confirmation clears and nothing starts`() = runTest {
        // Arrange
        val model = createContentModel()
        content(model).onActionClick(DEVICE_REVOKE)
        advanceUntilIdle()

        // Act
        content(model).revokeConfirmation?.onDismiss?.invoke()
        advanceUntilIdle()

        // Assert
        assertThat(content(model).revokeConfirmation).isNull()
        verify(exactly = 0) { startOperation(any(), any()) }
    }

    @Test
    fun `GIVEN a dismissed revoke confirmation WHEN its hold completes late THEN nothing starts`() = runTest {
        // Arrange
        val model = createContentModel()
        content(model).onActionClick(UNKNOWN_SIGNER_REVOKE)
        advanceUntilIdle()
        val confirmation = content(model).revokeConfirmation
        confirmation?.onDismiss?.invoke()
        advanceUntilIdle()

        // Act
        confirmation?.onConfirm?.invoke()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { startOperation(any(), any()) }
    }

    @Test
    fun `GIVEN a second confirmation opened WHEN the first one dismisses late THEN the second one stays`() = runTest {
        // Arrange
        val model = createContentModel()
        content(model).onActionClick(DEVICE_REVOKE)
        advanceUntilIdle()
        val firstOnDismiss = content(model).revokeConfirmation?.onDismiss
        content(model).onActionClick(UNKNOWN_SIGNER_REVOKE)
        advanceUntilIdle()
        val second = content(model).revokeConfirmation

        // Act
        firstOnDismiss?.invoke()
        advanceUntilIdle()

        // Assert
        assertThat(content(model).revokeConfirmation).isSameInstanceAs(second)
    }

    @Test
    fun `GIVEN revoke confirmation WHEN an operation starts and finishes THEN confirmation does not reappear`() =
        runTest {
            // Arrange
            val model = createContentModel()
            content(model).onActionClick(DEVICE_REVOKE)
            advanceUntilIdle()

            // Act
            states.emit(createState(OVERVIEW.right(), runningOperation(PolymarketSessionKeyOperationRequest.Extend)))
            advanceUntilIdle()
            val whileRunning = content(model).revokeConfirmation
            states.emit(createState(OVERVIEW.right()))
            advanceUntilIdle()

            // Assert
            assertThat(whileRunning).isNull()
            assertThat(content(model).revokeConfirmation).isNull()
        }

    @Test
    fun `GIVEN revoke confirmation WHEN the key is revoked elsewhere THEN confirmation clears`() = runTest {
        // Arrange
        val model = createContentModel()
        content(model).onActionClick(DEVICE_REVOKE)
        advanceUntilIdle()

        // Act
        states.emit(createState(OVERVIEW.copy(wallet = OVERVIEW.wallet.copy(sessionKeys = emptyList())).right()))
        advanceUntilIdle()

        // Assert
        assertThat(content(model).revokeConfirmation).isNull()
    }

    @Test
    fun `GIVEN revoke confirmation WHEN overview fails and recovers THEN confirmation does not reappear`() = runTest {
        // Arrange
        val model = createContentModel()
        content(model).onActionClick(DEVICE_REVOKE)
        advanceUntilIdle()

        // Act
        states.emit(createState(PolymarketWalletError.Network.left()))
        advanceUntilIdle()
        states.emit(createState(OVERVIEW.right()))
        advanceUntilIdle()

        // Assert
        assertThat(content(model).revokeConfirmation).isNull()
    }

    @Test
    fun `GIVEN another operation started meanwhile WHEN revoke confirmed THEN busy toast is shown`() = runTest {
        // Arrange
        every { startOperation(USER_WALLET_ID, REVOKE) } returns PolymarketSessionKeyOperationStart.Busy
        val model = createContentModel()
        content(model).onActionClick(DEVICE_REVOKE)
        advanceUntilIdle()

        // Act
        content(model).revokeConfirmation?.onConfirm?.invoke()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { startOperation(USER_WALLET_ID, REVOKE) }
        val toast = sentMessages.single() as SnackbarMessage
        assertThat(toast.message).isEqualTo(resourceReference(R.string.prediction_session_key_toast_busy))
    }

    @Test
    fun `GIVEN an operation started meanwhile WHEN revoke confirmed THEN nothing starts nor is toasted`() = runTest {
        // Arrange
        val model = createContentModel()
        content(model).onActionClick(DEVICE_REVOKE)
        advanceUntilIdle()
        val onConfirm = content(model).revokeConfirmation?.onConfirm
        states.emit(createState(OVERVIEW.right(), runningOperation(PolymarketSessionKeyOperationRequest.Extend)))
        advanceUntilIdle()

        // Act
        onConfirm?.invoke()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { startOperation(any(), any()) }
        assertThat(sentMessages).isEmpty()
    }

    @Test
    fun `GIVEN the key disappeared meanwhile WHEN revoke confirmed THEN nothing starts`() = runTest {
        // Arrange
        val model = createContentModel()
        content(model).onActionClick(DEVICE_REVOKE)
        advanceUntilIdle()
        val onConfirm = content(model).revokeConfirmation?.onConfirm
        states.emit(createState(OVERVIEW.copy(wallet = OVERVIEW.wallet.copy(sessionKeys = emptyList())).right()))
        advanceUntilIdle()

        // Act
        onConfirm?.invoke()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { startOperation(any(), any()) }
    }

    @Test
    fun `GIVEN the key address changed only its case meanwhile WHEN revoke confirmed THEN revocation starts`() =
        runTest {
            // Arrange
            val model = createContentModel()
            content(model).onActionClick(DEVICE_REVOKE)
            advanceUntilIdle()
            val onConfirm = content(model).revokeConfirmation?.onConfirm
            val recasedKeys = OVERVIEW.wallet.sessionKeys.map {
                it.copy(address = "0x" + KEY_ADDRESS.removePrefix("0x").uppercase())
            }
            states.emit(createState(OVERVIEW.copy(wallet = OVERVIEW.wallet.copy(sessionKeys = recasedKeys)).right()))
            advanceUntilIdle()

            // Act
            onConfirm?.invoke()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { startOperation(USER_WALLET_ID, REVOKE) }
        }

    @ParameterizedTest
    @MethodSource("provideRevokePathModels")
    fun `GIVEN wallet type and signer WHEN revoke clicked THEN it starts directly or asks confirmation`(
        model: RevokePathModel,
    ) = runTest {
        // Arrange
        every { getUserWallet(USER_WALLET_ID) } returns model.wallet.right()
        val settingsModel = createContentModel()

        // Act
        content(settingsModel).onActionClick(model.action)
        advanceUntilIdle()

        // Assert
        assertThat(content(settingsModel).revokeConfirmation?.isHoldToConfirm).isEqualTo(model.expectedHoldToConfirm)
        verify(exactly = if (model.expectedHoldToConfirm == null) 1 else 0) { startOperation(USER_WALLET_ID, REVOKE) }
    }

    @ParameterizedTest
    @MethodSource("provideRevokePathModels")
    fun `GIVEN wallet type and signer WHEN revoke is confirmed or direct THEN revocation starts once and clears`(
        model: RevokePathModel,
    ) = runTest {
        // Arrange
        every { getUserWallet(USER_WALLET_ID) } returns model.wallet.right()
        val settingsModel = createContentModel()
        content(settingsModel).onActionClick(model.action)
        advanceUntilIdle()

        // Act
        content(settingsModel).revokeConfirmation?.onConfirm?.invoke()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { startOperation(USER_WALLET_ID, REVOKE) }
        assertThat(content(settingsModel).revokeConfirmation).isNull()
    }

    @Test
    fun `GIVEN card wallet and device key and revoke not offered WHEN revoke clicked THEN nothing starts`() =
        runTest {
            // Arrange
            every { getUserWallet(USER_WALLET_ID) } returns COLD_WALLET.right()
            val model = createModel()
            states.emit(createState(OVERVIEW.right(), runningOperation(PolymarketSessionKeyOperationRequest.Extend)))
            advanceUntilIdle()

            // Act
            content(model).onActionClick(DEVICE_REVOKE)
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { startOperation(any(), any()) }
            assertThat(content(model).revokeConfirmation).isNull()
        }

    @Test
    fun `GIVEN wallet lookup fails WHEN revoke clicked THEN nothing happens`() = runTest {
        // Arrange
        every { getUserWallet(USER_WALLET_ID) } returns GetUserWalletError.UserWalletNotFound.left()
        val model = createContentModel()

        // Act
        content(model).onActionClick(DEVICE_REVOKE)
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { startOperation(any(), any()) }
        assertThat(content(model).revokeConfirmation).isNull()
    }

    @Test
    fun `GIVEN activation offered WHEN activate clicked THEN activation starts without a toast`() = runTest {
        // Arrange
        every { startOperation(USER_WALLET_ID, PolymarketSessionKeyOperationRequest.Activate) } returns
            PolymarketSessionKeyOperationStart.Started(OPERATION_ID)
        val model = createContentModel()

        // Act
        content(model).onActivateClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { startOperation(USER_WALLET_ID, PolymarketSessionKeyOperationRequest.Activate) }
        assertThat(sentMessages).isEmpty()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN banner not actionable WHEN activate clicked THEN nothing starts`(state: PolymarketFastTradingState) =
        runTest {
            // Arrange
            val model = createModel()
            states.emit(createState(OVERVIEW.right()))
            advanceUntilIdle()
            val onActivateClick = content(model).onActivateClick
            states.emit(state)
            advanceUntilIdle()

            // Act
            onActivateClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { startOperation(any(), any()) }
            assertThat(sentMessages).isEmpty()
        }

    @Test
    fun `GIVEN another operation running WHEN activate clicked THEN busy toast is shown`() = runTest {
        // Arrange
        every { startOperation(USER_WALLET_ID, PolymarketSessionKeyOperationRequest.Activate) } returns
            PolymarketSessionKeyOperationStart.Busy
        val model = createContentModel()

        // Act
        content(model).onActivateClick()
        advanceUntilIdle()

        // Assert
        val toast = sentMessages.single() as SnackbarMessage
        assertThat(toast.message).isEqualTo(resourceReference(R.string.prediction_session_key_toast_busy))
    }

    @ParameterizedTest
    @MethodSource("provideThisDeviceActionModels")
    fun `GIVEN this device action offered WHEN clicked THEN its operation starts without a toast`(
        model: ThisDeviceActionModel,
    ) = runTest {
        // Arrange
        every { startOperation(USER_WALLET_ID, model.request) } returns
            PolymarketSessionKeyOperationStart.Started(OPERATION_ID)
        val settingsModel = createModel()
        states.emit(createState(model.overview.right()))
        advanceUntilIdle()

        // Act
        content(settingsModel).onActionClick(model.action)
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { startOperation(USER_WALLET_ID, model.request) }
        assertThat(sentMessages).isEmpty()
    }

    @ParameterizedTest
    @MethodSource("provideThisDeviceActionModels")
    fun `GIVEN this device action not offered WHEN clicked THEN nothing starts`(model: ThisDeviceActionModel) =
        runTest {
            // Arrange
            val settingsModel = createContentModel()

            // Act
            content(settingsModel).onActionClick(model.action)
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { startOperation(any(), any()) }
        }

    @Test
    fun `GIVEN extend offered and an operation running WHEN extend clicked THEN nothing starts`() = runTest {
        // Arrange
        val settingsModel = createModel()
        states.emit(createState(EXPIRING_OVERVIEW.right(), runningOperation(REVOKE)))
        advanceUntilIdle()

        // Act
        content(settingsModel).onActionClick(FastTradingKeyActionUM.Extend)
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { startOperation(any(), any()) }
    }

    @Test
    fun `GIVEN another operation started meanwhile WHEN extend clicked THEN busy toast is shown`() = runTest {
        // Arrange
        every { startOperation(USER_WALLET_ID, PolymarketSessionKeyOperationRequest.Extend) } returns
            PolymarketSessionKeyOperationStart.Busy
        val settingsModel = createModel()
        states.emit(createState(EXPIRING_OVERVIEW.right()))
        advanceUntilIdle()

        // Act
        content(settingsModel).onActionClick(FastTradingKeyActionUM.Extend)
        advanceUntilIdle()

        // Assert
        val toast = sentMessages.single() as SnackbarMessage
        assertThat(toast.message).isEqualTo(resourceReference(R.string.prediction_session_key_toast_busy))
    }

    private suspend fun TestScope.createContentModel(): PolymarketFastTradingSettingsModel {
        val model = createModel()
        states.emit(createState(OVERVIEW.right()))
        advanceUntilIdle()
        return model
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
        getUserWallet = getUserWallet,
    ).also { model = it }

    data class ThisDeviceActionModel(
        val overview: PolymarketSessionKeyOverview,
        val action: FastTradingKeyActionUM,
        val request: PolymarketSessionKeyOperationRequest,
    )

    data class RevokePathModel(
        val wallet: UserWallet,
        val action: FastTradingKeyActionUM.Revoke,
        val expectedHoldToConfirm: Boolean?,
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        val HOT_WALLET: UserWallet = mockk<UserWallet.Hot>()
        val COLD_WALLET: UserWallet = mockk<UserWallet.Cold>()
        const val KEY_ADDRESS = "0x5e55000000000000000000000000000000000002"
        const val OWN_ADDRESS = "0x5e55000000000000000000000000000000000001"
        const val OPERATION_ID = 5L
        val REVOKE = PolymarketSessionKeyOperationRequest.Revoke(KEY_ADDRESS)
        val DEVICE_NAME = stringReference("Galaxy S24")
        val DEVICE_REVOKE = FastTradingKeyActionUM.Revoke(
            address = KEY_ADDRESS,
            subject = FastTradingKeyActionUM.RevokeSubject.Device(DEVICE_NAME),
        )
        val UNKNOWN_SIGNER_REVOKE = FastTradingKeyActionUM.Revoke(
            address = KEY_ADDRESS,
            subject = FastTradingKeyActionUM.RevokeSubject.UnknownSigner,
        )
        val OVERVIEW = PolymarketSessionKeyOverview(
            wallet = PolymarketWalletState(
                depositWalletAddress = "0x2222222222222222222222222222222222222222",
                status = PolymarketWalletStatus.READY_TO_TRADE,
                sessionKeys = listOf(
                    PolymarketSessionKey(
                        address = KEY_ADDRESS,
                        deviceId = "other-device",
                        deviceName = "Galaxy S24",
                        isEnabled = true,
                        status = PolymarketSessionKeyStatus.ACTIVE,
                        scopes = listOf("CLOB"),
                        validUntil = Instant.now().plus(Duration.ofDays(100)),
                        renewAfter = null,
                        authorizedAt = null,
                        createdAt = Instant.now(),
                    ),
                ),
            ),
            localKeyAddress = null,
            deviceId = "this-device",
            assessment = PolymarketSessionKeyAssessment(
                deviceState = PolymarketSessionKeyDeviceState.OfferEnable,
                reconciliation = PolymarketSessionKeyReconciliation.Unavailable,
                nextChangeAt = null,
            ),
            isTradable = false,
        )

        val OWN_KEY = OVERVIEW.wallet.sessionKeys.single().copy(
            address = OWN_ADDRESS,
            deviceId = "this-device",
            deviceName = "Pixel 9",
            validUntil = Instant.now().plus(Duration.ofDays(10)),
        )
        val EXPIRING_OVERVIEW = OVERVIEW.copy(
            wallet = OVERVIEW.wallet.copy(sessionKeys = listOf(OWN_KEY)),
            localKeyAddress = OWN_ADDRESS,
            assessment = OVERVIEW.assessment.copy(
                deviceState = PolymarketSessionKeyDeviceState.Active(OWN_KEY, isRenewDue = true),
            ),
        )
        val EXPIRED_OWN_KEY = OWN_KEY.copy(
            status = PolymarketSessionKeyStatus.EXPIRED,
            validUntil = Instant.now().minus(Duration.ofDays(1)),
        )
        val EXPIRED_OVERVIEW = EXPIRING_OVERVIEW.copy(
            wallet = OVERVIEW.wallet.copy(sessionKeys = listOf(EXPIRED_OWN_KEY)),
            assessment = OVERVIEW.assessment.copy(
                deviceState = PolymarketSessionKeyDeviceState.Reenable(EXPIRED_OWN_KEY),
            ),
        )

        @JvmStatic
        fun provideRevokePathModels() = listOf(
            RevokePathModel(COLD_WALLET, DEVICE_REVOKE, expectedHoldToConfirm = null),
            RevokePathModel(COLD_WALLET, UNKNOWN_SIGNER_REVOKE, expectedHoldToConfirm = false),
            RevokePathModel(HOT_WALLET, DEVICE_REVOKE, expectedHoldToConfirm = true),
            RevokePathModel(HOT_WALLET, UNKNOWN_SIGNER_REVOKE, expectedHoldToConfirm = true),
        )

        @JvmStatic
        fun provideThisDeviceActionModels() = listOf(
            ThisDeviceActionModel(
                overview = EXPIRING_OVERVIEW,
                action = FastTradingKeyActionUM.Extend,
                request = PolymarketSessionKeyOperationRequest.Extend,
            ),
            ThisDeviceActionModel(
                overview = EXPIRED_OVERVIEW,
                action = FastTradingKeyActionUM.Reactivate,
                request = PolymarketSessionKeyOperationRequest.Reactivate,
            ),
        )

        @JvmStatic
        fun provideTestModels() = listOf(
            createState(
                OVERVIEW.copy(
                    assessment = OVERVIEW.assessment.copy(
                        deviceState = PolymarketSessionKeyDeviceState.Active(
                            sessionKey = OVERVIEW.wallet.sessionKeys.single(),
                            isRenewDue = false,
                        ),
                    ),
                ).right(),
            ),
            createState(OVERVIEW.right(), runningOperation(PolymarketSessionKeyOperationRequest.Activate)),
            createState(OVERVIEW.right(), runningOperation(REVOKE)),
        )

        fun runningOperation(request: PolymarketSessionKeyOperationRequest) = PolymarketSessionKeyOperation(
            id = OPERATION_ID,
            request = request,
            targetAddress = null,
            stage = PolymarketSessionKeyOperationStage.SIGNING,
        )

        fun createState(
            overview: Either<PolymarketWalletError, PolymarketSessionKeyOverview>,
            operation: PolymarketSessionKeyOperation? = null,
        ) = PolymarketFastTradingState(overview = overview, operation = operation, finished = null)
    }
}