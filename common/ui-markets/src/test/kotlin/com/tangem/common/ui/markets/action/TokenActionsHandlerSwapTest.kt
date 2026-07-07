package com.tangem.common.ui.markets.action

import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.utils.Provider
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class TokenActionsHandlerSwapTest {

    private val router: Router = mockk(relaxed = true)

    private val handler = TokenActionsHandler(
        router = router,
        clipboardManager = mockk(relaxed = true),
        uiMessageSender = mockk(relaxed = true),
        getOfframpUrlUseCase = mockk(relaxed = true),
        urlOpener = mockk(relaxed = true),
        analyticsEventHandler = mockk(relaxed = true),
        currentAppCurrency = Provider { mockk(relaxed = true) },
        onHandleQuickAction = { _, _ -> },
        coroutineScope = CoroutineScope(UnconfinedTestDispatcher()),
        isDemoCardUseCase = mockk(relaxed = true),
        isWalletBackupProblematicUseCase = mockk(relaxed = true),
        sendBackupProblemEmailUseCase = mockk(relaxed = true),
        messageSender = mockk(relaxed = true),
    )

    private val userWallet: UserWallet = mockk(relaxed = true)
    private val currency: CryptoCurrency = MockCryptoCurrencyFactory().ethereum
    private val data = CryptoCurrencyData(
        userWallet = userWallet,
        status = CryptoCurrencyStatus(currency = currency, value = CryptoCurrencyStatus.Loading),
        actions = emptyList(),
        isAccountMode = false,
        account = mockk<AccountStatus.CryptoPortfolio>(relaxed = true),
    )

    @Test
    fun `GIVEN AddFunds context WHEN handle Exchange THEN swap pushed with TO position`() {
        // Arrange
        val slot = slot<Route>()
        every { router.push(capture(slot), any()) } returns Unit

        // Act
        handler.handle(
            action = TokenActionsBSContentUM.Action.Exchange,
            cryptoCurrencyData = data,
            context = TokenActionsContext.AddFunds,
        )

        // Assert
        val pushed = slot.captured as AppRoute.Swap
        assertThat(pushed.fromCurrencyPosition).isEqualTo(AppRoute.Swap.CurrencyPosition.TO)
    }

    @Test
    fun `GIVEN Transfer context WHEN handle Exchange THEN swap pushed with FROM position`() {
        // Arrange
        val slot = slot<Route>()
        every { router.push(capture(slot), any()) } returns Unit

        // Act
        handler.handle(
            action = TokenActionsBSContentUM.Action.Exchange,
            cryptoCurrencyData = data,
            context = TokenActionsContext.Transfer,
        )

        // Assert
        val pushed = slot.captured as AppRoute.Swap
        assertThat(pushed.fromCurrencyPosition).isEqualTo(AppRoute.Swap.CurrencyPosition.FROM)
    }

    @Test
    fun `GIVEN default context WHEN handle Exchange THEN swap pushed with ANY position`() {
        // Arrange
        val slot = slot<Route>()
        every { router.push(capture(slot), any()) } returns Unit

        // Act
        handler.handle(
            action = TokenActionsBSContentUM.Action.Exchange,
            cryptoCurrencyData = data,
        )

        // Assert
        val pushed = slot.captured as AppRoute.Swap
        assertThat(pushed.fromCurrencyPosition).isEqualTo(AppRoute.Swap.CurrencyPosition.ANY)
    }
}