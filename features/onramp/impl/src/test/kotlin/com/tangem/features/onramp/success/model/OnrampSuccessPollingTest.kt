package com.tangem.features.onramp.success.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.onramp.GetOnrampStatusUseCase
import com.tangem.domain.onramp.GetOnrampTransactionUseCase
import com.tangem.domain.onramp.OnrampRemoveTransactionUseCase
import com.tangem.domain.onramp.analytics.OnrampAnalyticsEvent
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.onramp.model.OnrampStatus
import com.tangem.domain.onramp.model.cache.OnrampTransaction
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.onramp.OnrampModelTestBase
import com.tangem.features.onramp.component.OnrampSuccessComponent
import com.tangem.features.onramp.success.entity.OnrampSuccessComponentUM
import com.tangem.test.mock.MockAccounts
import io.mockk.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class OnrampSuccessPollingTest : OnrampModelTestBase() {

    private val wallet: UserWallet.Hot = mockk(relaxed = true)
    private val getTransaction: GetOnrampTransactionUseCase = mockk()
    private val getWallet: GetUserWalletUseCase = mockk()
    private val accounts: SingleAccountListSupplier = mockk()
    private val getStatus: GetOnrampStatusUseCase = mockk()
    private val removeTransaction: OnrampRemoveTransactionUseCase = mockk()
    private val messages: UiMessageSender = mockk(relaxUnitFun = true)
    private val currencyId = CryptoCurrency.ID.fromValue("coin⟨ethereum⟩ethereum")

    @ParameterizedTest
    @EnumSource(
        value = OnrampStatus.Status::class,
        names = ["Expired", "Paused", "Failed", "Finished", "Refunded"],
    )
    fun `GIVEN pending transaction WHEN terminal status arrives THEN polling stops and removal occurs once`(
        terminal: OnrampStatus.Status,
    ) = runTest {
        // Arrange
        val pending = createStatus()
        coEvery { getStatus(wallet, "tx") } returnsMany listOf(pending.right(), createStatus(terminal).right())
        coEvery { removeTransaction("tx") } returns Unit.right()
        val model = createModel()

        // Act
        runCurrent()
        assertThat((model.state.value as OnrampSuccessComponentUM.Content).activeStatus).isEqualTo(pending.status)
        coVerify(exactly = 0) { removeTransaction(any()) }
        advanceTimeBy(9_999)
        runCurrent()
        coVerify(exactly = 1) { getStatus(wallet, "tx") }
        advanceTimeBy(1)
        runCurrent()
        advanceTimeBy(60_000)
        runCurrent()

        // Assert
        assertThat((model.state.value as OnrampSuccessComponentUM.Content).activeStatus).isEqualTo(terminal)
        coVerify(exactly = 2) { getStatus(wallet, "tx") }
        coVerify(exactly = 1) { removeTransaction("tx") }
        verify(exactly = 1) {
            analyticsEventHandler.send(OnrampAnalyticsEvent.SuccessScreenOpened("Provider", "USD", "ETH", "US", "card"))
        }
        verify(exactly = 0) { messages.send(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN failed status request WHEN next poll succeeds THEN existing content preserved and error reported`() =
        runTest {
            // Arrange
            val error = OnrampError.DataError("500", "offline")
            coEvery { getStatus(wallet, "tx") } returnsMany listOf(
                createStatus().right(), error.left(), createStatus(OnrampStatus.Status.Sending).right(),
            )
            val model = createModel()
            runCurrent()
            val initial = model.state.value

            // Act
            advanceTimeBy(10_000)
            runCurrent()
            assertThat(model.state.value).isEqualTo(initial)
            advanceTimeBy(10_000)
            runCurrent()

            // Assert
            assertThat((model.state.value as OnrampSuccessComponentUM.Content).activeStatus)
                .isEqualTo(OnrampStatus.Status.Sending)
            verify(exactly = 1) { messages.send(ofType<DialogMessage>()) }
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    OnrampAnalyticsEvent.Errors(
                        tokenSymbol = "ETH", providerName = "Provider", paymentMethod = "card", errorCode = "500",
                    ),
                )
            }
            coVerify(exactly = 3) { getStatus(wallet, "tx") }
            coVerify(exactly = 0) { removeTransaction(any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN suspended status request WHEN model destroyed THEN request cancelled and no late effects occur`() = runTest {
        // Arrange
        val cancelled = CompletableDeferred<Unit>()
        coEvery { getStatus(wallet, "tx") } coAnswers {
            try {
                awaitCancellation()
            } finally {
                cancelled.complete(Unit)
            }
        }
        val model = createModel()
        runCurrent()

        // Act
        model.onDestroy()
        runCurrent()
        advanceTimeBy(30_000)
        runCurrent()

        // Assert
        assertThat(cancelled.isCompleted).isTrue()
        assertThat(model.state.value).isEqualTo(OnrampSuccessComponentUM.Loading)
        coVerify(exactly = 1) { getStatus(wallet, "tx") }
        coVerify(exactly = 0) { removeTransaction(any()) }
        verify(exactly = 0) { messages.send(any()) }
        verify(exactly = 0) { analyticsEventHandler.send(ofType<OnrampAnalyticsEvent.Errors>()) }
    }

    private fun TestScope.createModel(): OnrampSuccessComponentModel {
        every { cryptoCurrency.id } returns currencyId
        every { cryptoCurrency.name } returns "Ethereum"
        every { cryptoCurrency.symbol } returns "ETH"
        every { cryptoCurrency.decimals } returns 18
        val account = MockAccounts.createAccount(0, cryptoCurrencies = listOf(cryptoCurrency), userWalletId = userWalletId)
        val accountList = AccountList(userWalletId, listOf(account), 1, 0, 0).getOrNull()!!
        coEvery { getTransaction("tx") } returns createTransaction().right()
        every { getWallet(userWalletId) } returns wallet.right()
        coEvery { accounts.getSyncOrNull(userWalletId) } returns accountList
        return OnrampSuccessComponentModel(
            dispatchers = testDispatchers(), urlOpener = mockk(), getOnrampTransactionUseCase = getTransaction,
            getOnrampStatusUseCase = getStatus, singleAccountListSupplier = accounts,
            onrampRemoveTransactionUseCase = removeTransaction, getUserWalletUseCase = getWallet,
            analyticsEventHandler = analyticsEventHandler, messageSender = messages,
            clipboardManager = mockk(), router = router,
            paramsContainer = MutableParamsContainer(OnrampSuccessComponent.Params("tx")),
        )
    }

    private fun createTransaction() = OnrampTransaction(
        txId = "tx", userWalletId = userWalletId, fromAmount = BigDecimal.TEN,
        fromCurrency = OnrampCurrency("Dollar", "USD", null, 2, "$"),
        toAmount = BigDecimal("0.01"), toCurrencyId = currencyId.value,
        status = OnrampStatus.Status.Created, externalTxUrl = null, externalTxId = null,
        timestamp = 0, providerName = "Provider", providerImageUrl = "", providerType = "provider",
        redirectUrl = "", paymentMethod = "card", residency = "US",
    )

    private fun createStatus(status: OnrampStatus.Status = OnrampStatus.Status.WaitingForPayment) = OnrampStatus(
        txId = "tx", providerId = "provider", payoutAddress = "address", status = status,
        failReason = null, externalTxId = null, externalTxUrl = null, payoutHash = null,
        createdAt = "2026-01-01T00:00:00Z", fromCurrencyCode = "USD", fromAmount = "1000",
        toContractAddress = "0", toNetwork = "ethereum", toDecimals = "18", toAmount = null,
        toActualAmount = null, paymentMethod = "card", countryCode = "US",
    )
}