package com.tangem.features.walletconnect.connections.model

import android.text.format.DateFormat
import arrow.core.left
import arrow.core.right
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.core.utils.lceContent
import com.tangem.domain.mcp.models.McpSession
import com.tangem.domain.mcp.models.analytics.McpSessionsAnalyticsEvent
import com.tangem.domain.mcp.repository.McpRepository
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.walletconnect.connections.components.McpSessionInfoComponent
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class McpSessionInfoModelTest {

    private val mcpRepository: McpRepository = mockk()
    private val messageSender: UiMessageSender = mockk(relaxed = true)
    private val analytics: AnalyticsEventHandler = mockk(relaxUnitFun = true)

    @BeforeAll
    fun mockDateFormat() {
        // DateTimeFormatters delegates to android.text.format.DateFormat, an Android stub unavailable on the JVM
        mockkStatic(DateFormat::class)
        every { DateFormat.getBestDateTimePattern(any(), any()) } answers { secondArg() }
    }

    @AfterAll
    fun unmockDateFormat() {
        unmockkStatic(DateFormat::class)
    }

    @BeforeEach
    fun setUp() {
        clearMocks(mcpRepository, messageSender, analytics)
        every { mcpRepository.getSessions() } returns flowOf(listOf(session()).lceContent())
    }

    @Test
    fun `GIVEN revoke succeeds WHEN revoke clicked THEN agent disconnected is sent`() = runTest {
        // Arrange
        coEvery { mcpRepository.revokeSession(SESSION_ID) } returns Unit.right()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.uiState.value!!.onRevokeClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { analytics.send(McpSessionsAnalyticsEvent.AgentDisconnected(session())) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN revoke fails WHEN revoke clicked THEN nothing is sent`() = runTest {
        // Arrange
        coEvery { mcpRepository.revokeSession(SESSION_ID) } returns IOException().left()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.uiState.value!!.onRevokeClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { analytics.send(any()) }
        model.onDestroy()
    }

    private fun createModel(testScope: TestScope) = McpSessionInfoModel(
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        mcpRepository = mcpRepository,
        messageSender = messageSender,
        analytics = analytics,
        paramsContainer = MutableParamsContainer(
            McpSessionInfoComponent.Params(sessionId = SESSION_ID, walletName = "Wallet", onDismiss = {}),
        ),
    )

    private fun session() = McpSession(
        id = SESSION_ID,
        walletId = UserWalletId("aabbcc"),
        createdAtMillis = 0L,
        clientId = "client-id",
        clientOrigin = AGENT_ORIGIN,
        clientName = "Claude",
        clientIconUrl = null,
        scopes = emptyList(),
        clientIp = null,
        clientCountry = null,
        clientUserAgent = null,
    )

    private companion object {
        const val SESSION_ID = "session-id"
        const val AGENT_ORIGIN = "claude.ai"
    }
}