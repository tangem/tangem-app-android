package com.tangem.common.ui.markets.action

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.event.JointAccountFakedoorAnalyticsEvent
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.utils.Provider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TokenActionsHandlerUnavailabilityTest {

    private val router: Router = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val handledActions = mutableListOf<TokenActionsHandler.HandledQuickAction>()

    private val handler = TokenActionsHandler(
        router = router,
        clipboardManager = mockk(relaxed = true),
        uiMessageSender = uiMessageSender,
        getOfframpUrlUseCase = mockk(relaxed = true),
        urlOpener = mockk(relaxed = true),
        analyticsEventHandler = analyticsEventHandler,
        currentAppCurrency = Provider { mockk(relaxed = true) },
        onHandleQuickAction = { handled, _ -> handledActions.add(handled) },
        coroutineScope = CoroutineScope(UnconfinedTestDispatcher()),
        isDemoCardUseCase = mockk(relaxed = true),
        backupErrorWarningSender = mockk {
            every { forWallet(any(), any(), any(), any(), any()) } answers { lastArg<() -> Unit>().invoke() }
        },
        messageSender = mockk(relaxed = true),
    )

    private val currency: CryptoCurrency = MockCryptoCurrencyFactory().ethereum

    @BeforeEach
    fun resetMocks() {
        clearMocks(router, uiMessageSender, analyticsEventHandler)
        handledActions.clear()
    }

    private fun data(actions: List<TokenActionsState.ActionState>) = CryptoCurrencyData(
        userWallet = mockk<UserWallet>(relaxed = true),
        status = CryptoCurrencyStatus(currency = currency, value = CryptoCurrencyStatus.Loading),
        actions = actions,
        isAccountMode = false,
        account = mockk<AccountStatus.Personal>(relaxed = true),
    )

    @Test
    fun `GIVEN sell not supported WHEN handle Sell THEN reason shown and flow not continued`() {
        // Arrange
        val data = data(
            actions = listOf(
                TokenActionsState.ActionState.Sell(
                    ScenarioUnavailabilityReason.NotSupportedBySellService(cryptoCurrencyName = "ETH"),
                ),
            ),
        )

        // Act
        handler.handle(
            action = TokenActionsBSContentUM.Action.Sell,
            cryptoCurrencyData = data,
            context = TokenActionsContext.Transfer,
        )

        // Assert
        verify { uiMessageSender.send(any<DialogMessage>()) }
        verify(exactly = 0) { router.push(any(), any()) }
        assertThat(handledActions).isEmpty()
    }

    @Test
    fun `GIVEN send with empty balance WHEN handle Send THEN reason shown and sheet not dismissed`() {
        // Arrange
        val data = data(
            actions = listOf(
                TokenActionsState.ActionState.Send(
                    ScenarioUnavailabilityReason.EmptyBalance(ScenarioUnavailabilityReason.WithdrawalScenario.SEND),
                ),
            ),
        )

        // Act
        handler.handle(
            action = TokenActionsBSContentUM.Action.Send,
            cryptoCurrencyData = data,
            context = TokenActionsContext.Transfer,
        )

        // Assert
        verify { uiMessageSender.send(any<DialogMessage>()) }
        verify(exactly = 0) { router.push(any(), any()) }
        assertThat(handledActions).isEmpty()
    }

    @Test
    fun `GIVEN send available WHEN handle Send THEN navigates and no reason shown`() {
        // Arrange
        val data = data(
            actions = listOf(TokenActionsState.ActionState.Send(ScenarioUnavailabilityReason.None)),
        )

        // Act
        handler.handle(
            action = TokenActionsBSContentUM.Action.Send,
            cryptoCurrencyData = data,
            context = TokenActionsContext.Transfer,
        )

        // Assert
        verify { router.push(any(), any()) }
        verify(exactly = 0) { uiMessageSender.send(any<DialogMessage>()) }
        assertThat(handledActions).isNotEmpty()
    }

    @ParameterizedTest
    @MethodSource("provideFakedoorModels")
    fun `GIVEN a joint account in Transfer WHEN a held-back action tapped THEN the fakedoor is reported`(
        model: FakedoorModel,
    ) {
        // Arrange
        val data = data(actions = jointHeldBackActions)

        // Act
        handler.handle(action = model.action, cryptoCurrencyData = data, context = TokenActionsContext.Transfer)

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(
                JointAccountFakedoorAnalyticsEvent(
                    source = JointAccountFakedoorAnalyticsEvent.Source.Transfer,
                    action = model.expected,
                    blockchain = currency.network.name,
                    token = currency.symbol,
                ),
            )
        }
    }

    data class FakedoorModel(
        val action: TokenActionsBSContentUM.Action,
        val expected: JointAccountFakedoorAnalyticsEvent.Action,
    )

    private fun provideFakedoorModels() = listOf(
        FakedoorModel(TokenActionsBSContentUM.Action.Exchange, JointAccountFakedoorAnalyticsEvent.Action.Swap),
        FakedoorModel(
            action = TokenActionsBSContentUM.Action.SendWithSwap,
            expected = JointAccountFakedoorAnalyticsEvent.Action.SwapAndSend,
        ),
        FakedoorModel(TokenActionsBSContentUM.Action.Sell, JointAccountFakedoorAnalyticsEvent.Action.Sell),
    )

    @Test
    fun `GIVEN another unavailability reason WHEN handle Sell THEN no fakedoor is reported`() {
        // Arrange
        val data = data(
            actions = listOf(
                TokenActionsState.ActionState.Sell(
                    ScenarioUnavailabilityReason.NotSupportedBySellService(cryptoCurrencyName = "ETH"),
                ),
            ),
        )

        // Act
        handler.handle(
            action = TokenActionsBSContentUM.Action.Sell,
            cryptoCurrencyData = data,
            context = TokenActionsContext.Transfer,
        )

        // Assert
        verify(exactly = 0) { analyticsEventHandler.send(ofType<JointAccountFakedoorAnalyticsEvent>()) }
    }

    private val jointHeldBackActions = listOf(
        TokenActionsState.ActionState.Swap(ScenarioUnavailabilityReason.JointAccount, shouldShowBadge = false),
        TokenActionsState.ActionState.Sell(ScenarioUnavailabilityReason.JointAccount),
    )
}