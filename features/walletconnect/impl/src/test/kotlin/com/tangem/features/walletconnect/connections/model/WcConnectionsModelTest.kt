package com.tangem.features.walletconnect.connections.model

import arrow.core.right
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessage
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.account.supplier.MultiAccountListSupplier
import com.tangem.domain.mcp.McpRequestService
import com.tangem.domain.mcp.repository.McpRepository
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.qrscanning.models.RawQrResult
import com.tangem.domain.qrscanning.usecases.ListenToQrScanningUseCase
import com.tangem.domain.walletconnect.ConnectionsAnalyticEvents
import com.tangem.domain.walletconnect.WcAnalyticEvents
import com.tangem.domain.walletconnect.WcPairService
import com.tangem.domain.walletconnect.usecase.WcSessionsUseCase
import com.tangem.domain.walletconnect.usecase.disconnect.WcDisconnectUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.walletconnect.connections.components.WcConnectionsComponent
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class WcConnectionsModelTest {

    private val router: Router = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val listenToQrScanningUseCase: ListenToQrScanningUseCase = mockk()
    private val wcSessionsUseCase: WcSessionsUseCase = mockk()
    private val getWalletsUseCase: GetWalletsUseCase = mockk()
    private val wcDisconnectUseCase: WcDisconnectUseCase = mockk()
    private val multiAccountListSupplier: MultiAccountListSupplier = mockk()
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase = mockk()
    private val wcPairService: WcPairService = mockk()
    private val mcpRequestService: McpRequestService = mockk()
    private val mcpRepository: McpRepository = mockk()
    private val analytics: AnalyticsEventHandler = mockk(relaxUnitFun = true)

    @BeforeEach
    fun setUp() {
        clearMocks(
            uiMessageSender,
            listenToQrScanningUseCase,
            wcSessionsUseCase,
            getWalletsUseCase,
            wcDisconnectUseCase,
            multiAccountListSupplier,
            isAccountsModeEnabledUseCase,
            mcpRepository,
            analytics,
        )
        every { listenToQrScanningUseCase.listen(any()) } returns emptyFlow<RawQrResult>().right()
        every { wcSessionsUseCase() } returns emptyFlow()
        every { getWalletsUseCase() } returns emptyFlow()
        every { isAccountsModeEnabledUseCase() } returns emptyFlow()
        every { multiAccountListSupplier(Unit) } returns emptyFlow()
        every { mcpRepository.getSessions() } returns emptyFlow()
        coEvery { mcpRepository.revokeAllSessions() } returns Unit
        coEvery { wcDisconnectUseCase.disconnectAll() } returns Unit
    }

    @Test
    fun `GIVEN connections screen WHEN screen opened THEN connections screen opened event is sent`() = runTest {
        // Act
        val connectionsModel = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { analytics.send(ConnectionsAnalyticEvents.ScreenOpened()) }
        verify(exactly = 0) { analytics.send(ofType<WcAnalyticEvents.ScreenOpened>()) }
        connectionsModel.onDestroy()
    }

    @Test
    fun `GIVEN disconnect all dialog WHEN confirmed THEN connections disconnect all event is sent`() = runTest {
        // Arrange
        val messages = mutableListOf<UiMessage>()
        every { uiMessageSender.send(capture(messages)) } returns Unit
        val connectionsModel = createModel(testScope = this)
        advanceUntilIdle()
        connectionsModel.uiState.value.topAppBarConfig.disconnectAllItem.onClick()

        // Act
        messages.filterIsInstance<DialogMessage>().single().firstAction.onClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { analytics.send(ConnectionsAnalyticEvents.ButtonDisconnectAll()) }
        verify(exactly = 0) { analytics.send(ofType<WcAnalyticEvents.ButtonDisconnectAll>()) }
        coVerify(exactly = 1) { mcpRepository.revokeAllSessions() }
        connectionsModel.onDestroy()
    }

    @Test
    fun `GIVEN disconnect all dialog WHEN cancelled THEN no disconnect all event is sent`() = runTest {
        // Arrange
        val messages = mutableListOf<UiMessage>()
        every { uiMessageSender.send(capture(messages)) } returns Unit
        val connectionsModel = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        connectionsModel.uiState.value.topAppBarConfig.disconnectAllItem.onClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { analytics.send(ofType<ConnectionsAnalyticEvents.ButtonDisconnectAll>()) }
        verify(exactly = 0) { analytics.send(ofType<WcAnalyticEvents.ButtonDisconnectAll>()) }
        connectionsModel.onDestroy()
    }

    private fun createModel(testScope: TestScope) = WcConnectionsModel(
        router = router,
        uiMessageSender = uiMessageSender,
        listenToQrScanningUseCase = listenToQrScanningUseCase,
        wcSessionsUseCase = wcSessionsUseCase,
        getWalletsUseCase = getWalletsUseCase,
        wcDisconnectUseCase = wcDisconnectUseCase,
        multiAccountListSupplier = multiAccountListSupplier,
        isAccountsModeEnabledUseCase = isAccountsModeEnabledUseCase,
        wcPairService = wcPairService,
        mcpRequestService = mcpRequestService,
        mcpRepository = mcpRepository,
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        analytics = analytics,
        paramsContainer = MutableParamsContainer(WcConnectionsComponent.Params(UserWalletId("aabbcc"))),
    )
}