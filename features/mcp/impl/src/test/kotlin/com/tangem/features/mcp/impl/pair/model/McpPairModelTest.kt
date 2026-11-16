package com.tangem.features.mcp.impl.pair.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.notifications.SystemNotificationsStateProvider
import com.tangem.core.navigation.settings.SettingsManager
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.mcp.models.McpDeviceVerification
import com.tangem.domain.mcp.models.McpDeviceVerificationResult
import com.tangem.domain.mcp.models.McpRequest
import com.tangem.domain.mcp.models.analytics.McpAuthorizationAnalyticsEvent
import com.tangem.domain.mcp.repository.McpRepository
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.domain.wallets.usecase.EnableAddressesSharingUseCase
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorController
import com.tangem.features.mcp.impl.R
import com.tangem.features.mcp.impl.pair.McpPairComponent
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.utils.coroutines.AppCoroutineScope
import io.mockk.MockKMatcherScope
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class McpPairModelTest {

    private val router: Router = mockk(relaxed = true)
    private val systemNotificationsStateProvider: SystemNotificationsStateProvider = mockk()
    private val settingsManager: SettingsManager = mockk(relaxed = true)
    private val mcpRepository: McpRepository = mockk()
    private val enableAddressesSharingUseCase: EnableAddressesSharingUseCase = mockk(relaxed = true)
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase = mockk()
    private val walletsRepository: WalletsRepository = mockk()
    private val messageSender: UiMessageSender = mockk(relaxed = true)
    private val analytics: AnalyticsEventHandler = mockk(relaxUnitFun = true)
    private val selectorController: PortfolioSelectorController = mockk(relaxUnitFun = true)
    private val portfolioFetcher: PortfolioFetcher = mockk()
    private val portfolioFetcherFactory: PortfolioFetcher.Factory = mockk()

    private val wallet: UserWallet = mockk<UserWallet.Cold> {
        every { walletId } returns WALLET_ID
        every { name } returns "Wallet"
    }

    private val otherWallet: UserWallet = mockk<UserWallet.Cold> {
        every { walletId } returns OTHER_WALLET_ID
        every { name } returns "Other wallet"
    }

    /** A wallet the fetcher does not offer, e.g. a single-currency one. */
    private val unofferedWallet: UserWallet = mockk<UserWallet.Cold> {
        every { walletId } returns UNOFFERED_WALLET_ID
        every { name } returns "Unoffered wallet"
    }

    private val selectedAccount = MutableSharedFlow<AccountId?>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    @BeforeEach
    fun setUp() {
        clearMocks(
            router,
            systemNotificationsStateProvider,
            mcpRepository,
            messageSender,
            analytics,
            selectorController,
            portfolioFetcher,
            portfolioFetcherFactory,
            walletsRepository,
            getSelectedWalletSyncUseCase,
        )
        selectedAccount.resetReplayCache()
        selectedAccount.tryEmit(ACCOUNT_ID)
        every { systemNotificationsStateProvider.areNotificationsEnabled() } returns true
        every { portfolioFetcherFactory.create(any(), any()) } returns portfolioFetcher
        every { portfolioFetcher.data } returns flowOf(portfolioData())
        every { selectorController.selectedAccountSync } returns ACCOUNT_ID
        every { selectorController.selectedAccount } returns selectedAccount
        coEvery { walletsRepository.isWalletRegistered(any()) } returns true
        every { selectedWalletOfTheApp() } returns wallet.right()
        every { selectorController.selectedAccountWithData(any()) } returns
            flowOf(wallet to mockk<AccountStatus.CryptoPortfolio>())
        coEvery { mcpRepository.getDeviceVerification(USER_CODE) } returns verification().right()
        coEvery { mcpRepository.approveDeviceVerification(any(), WALLET_ID) } returns
            McpDeviceVerificationResult(isSuccess = true).right()
        coEvery { mcpRepository.rejectDeviceVerification(any()) } returns Unit.right()
    }

    @Nested
    inner class Opening {

        @Test
        fun `GIVEN verification loads WHEN model created THEN received and requested are sent`() =
            runTest {
                // Act
                val model = createModel(testScope = this)
                advanceUntilIdle()

                // Assert
                verifyOrder {
                    analytics.send(McpAuthorizationAnalyticsEvent.ConnectionReceived())
                    analytics.send(McpAuthorizationAnalyticsEvent.AgentConnectionRequested(verification()))
                }
                verify(exactly = 1) { analytics.send(McpAuthorizationAnalyticsEvent.ConnectionReceived()) }
                verify(exactly = 1) {
                    analytics.send(McpAuthorizationAnalyticsEvent.AgentConnectionRequested(verification()))
                }
                verify(exactly = 0) { analytics.send(ofType<McpAuthorizationAnalyticsEvent.AuthorizationFailed>()) }
                model.onDestroy()
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ErrorCodes {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN verification fails WHEN model created THEN authorization failed carries its error code`(
            model: ErrorCodeModel,
        ) = runTest {
            // Arrange
            coEvery { mcpRepository.getDeviceVerification(USER_CODE) } returns model.error.left()

            // Act
            val pairModel = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { analytics.send(McpAuthorizationAnalyticsEvent.ConnectionReceived()) }
            verify(exactly = 1) {
                analytics.send(McpAuthorizationAnalyticsEvent.AuthorizationFailed(errorCode = model.expected))
            }
            verify(exactly = 0) {
                analytics.send(ofType<McpAuthorizationAnalyticsEvent.AgentConnectionRequested>())
            }
            pairModel.onDestroy()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN approval fails WHEN connect clicked THEN authorization failed carries its error code`(
            model: ErrorCodeModel,
        ) = runTest {
            // Arrange
            coEvery { mcpRepository.approveDeviceVerification(any(), WALLET_ID) } returns model.error.left()
            val pairModel = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            pairModel.agentInfo().connectButton.onClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) {
                analytics.send(McpAuthorizationAnalyticsEvent.AuthorizationFailed(errorCode = model.expected))
            }
            verify(exactly = 0) { analytics.send(ofType<McpAuthorizationAnalyticsEvent.AgentConnected>()) }
            pairModel.onDestroy()
        }

        private fun provideTestModels() = listOf(
            ErrorCodeModel(
                error = ApiResponseError.HttpException(
                    code = ApiResponseError.HttpException.Code.NOT_FOUND,
                    message = null,
                    errorBody = null,
                ),
                expected = "404",
            ),
            ErrorCodeModel(error = ApiResponseError.NetworkException(), expected = "network"),
            ErrorCodeModel(error = ApiResponseError.TimeoutException(), expected = "timeout"),
            ErrorCodeModel(error = IOException(), expected = "unknown"),
        )
    }

    @Nested
    inner class Approval {

        @Test
        fun `GIVEN service approves WHEN connect clicked THEN approve and connected are sent`() =
            runTest {
                // Arrange
                val model = createModel(testScope = this)
                advanceUntilIdle()

                // Act
                model.agentInfo().connectButton.onClick()
                advanceUntilIdle()

                // Assert
                verifyOrder {
                    analytics.send(McpAuthorizationAnalyticsEvent.ButtonApprove(verification()))
                    analytics.send(McpAuthorizationAnalyticsEvent.AgentConnected(verification()))
                }
                verify(exactly = 1) { analytics.send(McpAuthorizationAnalyticsEvent.ButtonApprove(verification())) }
                verify(exactly = 1) { analytics.send(McpAuthorizationAnalyticsEvent.AgentConnected(verification())) }
                verify(exactly = 0) { analytics.send(ofType<McpAuthorizationAnalyticsEvent.AuthorizationFailed>()) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN service refuses approval WHEN connect clicked THEN authorization failed is refused`() = runTest {
            // Arrange
            coEvery { mcpRepository.approveDeviceVerification(any(), WALLET_ID) } returns
                McpDeviceVerificationResult(isSuccess = false).right()
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.agentInfo().connectButton.onClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { analytics.send(McpAuthorizationAnalyticsEvent.ButtonApprove(verification())) }
            verify(exactly = 1) {
                analytics.send(McpAuthorizationAnalyticsEvent.AuthorizationFailed(errorCode = "refused"))
            }
            verify(exactly = 0) { analytics.send(ofType<McpAuthorizationAnalyticsEvent.AgentConnected>()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN notifications stay off WHEN permission result THEN push notice is sent`() =
            runTest {
                // Arrange
                every { systemNotificationsStateProvider.areNotificationsEnabled() } returns false
                val model = createModel(testScope = this)
                advanceUntilIdle()
                model.agentInfo().connectButton.onClick()
                advanceUntilIdle()

                // Act
                model.onPushPermissionResult()
                advanceUntilIdle()

                // Assert
                verify(exactly = 1) {
                    analytics.send(McpAuthorizationAnalyticsEvent.NoticePushPermissionRequired())
                }
                verify(exactly = 0) { analytics.send(ofType<McpAuthorizationAnalyticsEvent.AgentConnected>()) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN notifications turned on WHEN permission result THEN push notice is not sent`() =
            runTest {
                // Arrange
                every { systemNotificationsStateProvider.areNotificationsEnabled() } returns false
                val model = createModel(testScope = this)
                advanceUntilIdle()
                model.agentInfo().connectButton.onClick()
                advanceUntilIdle()
                every { systemNotificationsStateProvider.areNotificationsEnabled() } returns true

                // Act
                model.onPushPermissionResult()
                advanceUntilIdle()

                // Assert
                verify(exactly = 0) {
                    analytics.send(ofType<McpAuthorizationAnalyticsEvent.NoticePushPermissionRequired>())
                }
                verify(exactly = 1) { analytics.send(McpAuthorizationAnalyticsEvent.AgentConnected(verification())) }
                model.onDestroy()
            }
    }

    @Nested
    inner class UnregisteredWallet {

        @Test
        fun `GIVEN wallet not registered WHEN connect clicked THEN alert is shown and approval is not requested`() =
            runTest {
                // Arrange
                coEvery { walletsRepository.isWalletRegistered(WALLET_ID) } returns false
                val model = createModel(testScope = this)
                advanceUntilIdle()

                // Act
                model.agentInfo().connectButton.onClick()
                advanceUntilIdle()

                // Assert
                verify(exactly = 1) { messageSender.send(needsSyncAlert()) }
                coVerify(exactly = 0) { mcpRepository.approveDeviceVerification(any(), any()) }
                verify(exactly = 0) { analytics.send(ofType<McpAuthorizationAnalyticsEvent.AgentConnected>()) }
                assertThat(model.agentInfo().connectButton.isLoading).isFalse()
                model.onDestroy()
            }

        @Test
        fun `GIVEN wallet not registered WHEN picked in selector THEN alert is shown and the selector stays open`() =
            runTest {
                // Arrange
                coEvery { walletsRepository.isWalletRegistered(OTHER_WALLET_ID) } returns false
                val model = createModel(testScope = this)
                advanceUntilIdle()
                val stacks = trackStacks(model)

                // Act
                selectedAccount.tryEmit(OTHER_ACCOUNT_ID)
                advanceUntilIdle()

                // Assert
                verify(exactly = 1) { messageSender.send(needsSyncAlert()) }
                assertThat(stacks).isEmpty()
                model.onDestroy()
            }

        @Test
        fun `GIVEN registered wallet WHEN the selected one is picked again THEN the selector is closed`() = runTest {
            // Arrange
            val model = createModel(testScope = this)
            advanceUntilIdle()
            val stacks = trackStacks(model)

            // Act
            selectedAccount.tryEmit(ACCOUNT_ID)
            advanceUntilIdle()

            // Assert
            assertThat(stacks).containsExactly(listOf(McpPairRoutes.AgentInfo))
            verify(exactly = 0) { messageSender.send(needsSyncAlert()) }
            model.onDestroy()
        }
    }

    @Nested
    inner class InitialWallet {

        @Test
        fun `GIVEN the app wallet is offered WHEN model created THEN it is preselected`() = runTest {
            // Arrange
            every { selectorController.selectedAccountSync } returns null
            every { selectedWalletOfTheApp() } returns otherWallet.right()

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { selectorController.selectAccount(OTHER_ACCOUNT_ID) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN the app wallet is not offered WHEN model created THEN the first offered one is preselected`() =
            runTest {
                // Arrange
                every { selectorController.selectedAccountSync } returns null
                every { selectedWalletOfTheApp() } returns unofferedWallet.right()

                // Act
                val model = createModel(testScope = this)
                advanceUntilIdle()

                // Assert
                verify(exactly = 1) { selectorController.selectAccount(ACCOUNT_ID) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN no wallet is offered WHEN model created THEN the sheet is closed`() = runTest {
            // Arrange
            every { portfolioFetcher.data } returns flowOf(portfolioData(balances = emptyMap()))

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { router.pop(any()) }
            assertThat(model.uiState.value).isEqualTo(McpPairUM.Loading)
            model.onDestroy()
        }
    }

    @Nested
    inner class Decline {

        @Test
        fun `GIVEN agent shown WHEN decline clicked THEN decline is sent`() = runTest {
            // Arrange
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.agentInfo().onDecline()

            // Assert
            verify(exactly = 1) { analytics.send(McpAuthorizationAnalyticsEvent.ButtonDecline(verification())) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN agent shown WHEN sheet closed THEN decline is sent`() = runTest {
            // Arrange
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onClose()

            // Assert
            verify(exactly = 1) { analytics.send(McpAuthorizationAnalyticsEvent.ButtonDecline(verification())) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN agent connected WHEN sheet closed THEN decline is not sent`() = runTest {
            // Arrange
            val model = createModel(testScope = this)
            advanceUntilIdle()
            model.agentInfo().connectButton.onClick()
            advanceUntilIdle()

            // Act
            model.onClose()

            // Assert
            verify(exactly = 0) { analytics.send(ofType<McpAuthorizationAnalyticsEvent.ButtonDecline>()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN verification not loaded WHEN sheet closed THEN decline is not sent`() = runTest {
            // Arrange
            coEvery { mcpRepository.getDeviceVerification(USER_CODE) } returns IOException().left()
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onClose()

            // Assert
            verify(exactly = 0) { analytics.send(ofType<McpAuthorizationAnalyticsEvent.ButtonDecline>()) }
            model.onDestroy()
        }
    }

    private fun McpPairModel.agentInfo(): McpPairUM.AgentInfo = uiState.value as McpPairUM.AgentInfo

    private fun createModel(testScope: TestScope): McpPairModel = McpPairModel(
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        appScope = TestAppScope(testScope.backgroundScope),
        router = router,
        systemNotificationsStateProvider = systemNotificationsStateProvider,
        settingsManager = settingsManager,
        mcpRepository = mcpRepository,
        enableAddressesSharingUseCase = enableAddressesSharingUseCase,
        getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
        walletsRepository = walletsRepository,
        messageSender = messageSender,
        analytics = analytics,
        selectorController = selectorController,
        portfolioFetcherFactory = portfolioFetcherFactory,
        paramsContainer = MutableParamsContainer(McpPairComponent.Params(McpRequest.Pair(userCode = USER_CODE))),
    )

    private fun portfolioData(
        balances: Map<UserWalletId, AccountId> = mapOf(WALLET_ID to ACCOUNT_ID, OTHER_WALLET_ID to OTHER_ACCOUNT_ID),
    ): PortfolioFetcher.Data = mockk {
        every { this@mockk.balances } returns balances.mapValues { (_, accountId) ->
            mockk { every { accountsBalance.mainAccount.accountId } returns accountId }
        }
    }

    /** [GetSelectedWalletSyncUseCase.invoke] is deprecated, but the pairing deep link carries no wallet. */
    @Suppress("DEPRECATION")
    private fun MockKMatcherScope.selectedWalletOfTheApp() = getSelectedWalletSyncUseCase()

    private fun MockKMatcherScope.needsSyncAlert() = match<DialogMessage> { dialog ->
        (dialog.title as? TextReference.Res)?.id == R.string.mcp_connection_request_wallet_needs_sync_title
    }

    private fun trackStacks(model: McpPairModel): List<List<McpPairRoutes>> {
        val stacks = mutableListOf<List<McpPairRoutes>>()
        model.stackNavigation.subscribe { event ->
            stacks += event.transformer(listOf(McpPairRoutes.AgentInfo, McpPairRoutes.PortfolioSelector))
        }
        return stacks
    }

    private fun verification() = McpDeviceVerification(
        clientId = "client-id",
        clientOrigin = AGENT_ORIGIN,
        clientName = AGENT_NAME,
        clientIconUrl = null,
        scope = "scope",
        state = "state",
        userCode = USER_CODE,
    )

    data class ErrorCodeModel(val error: Throwable, val expected: String)

    private class TestAppScope(scope: CoroutineScope) : AppCoroutineScope, CoroutineScope by scope

    private companion object {
        const val USER_CODE = "user-code"
        const val AGENT_ORIGIN = "claude.ai"
        const val AGENT_NAME = "Claude"
        val WALLET_ID = UserWalletId("aabbcc")
        val OTHER_WALLET_ID = UserWalletId("ddeeff")
        val UNOFFERED_WALLET_ID = UserWalletId("001122")
        val ACCOUNT_ID = AccountId.forMainCryptoPortfolio(WALLET_ID)
        val OTHER_ACCOUNT_ID = AccountId.forMainCryptoPortfolio(OTHER_WALLET_ID)
    }
}