package com.tangem.features.moneyaccount.details

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.models.ReceiveAddressModel
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.analytics.MoneyAccountAnalyticsEvents
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusSupplier
import com.tangem.domain.wallets.usecase.GetExploreUrlUseCase
import com.tangem.features.moneyaccount.details.component.MoneyAccountDetailsComponent
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MoneyAccountDetailsModelTest {

    private val userWalletId = UserWalletId("1234567890ABCDEF")
    private val token: CryptoCurrency.Token = mockk()

    private val moneyAccountStatusSupplier: MoneyAccountStatusSupplier = mockk()
    private val router: Router = mockk(relaxed = true)
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase = mockk(relaxed = true)
    private val urlOpener: UrlOpener = mockk(relaxed = true)
    private val getExploreUrlUseCase: GetExploreUrlUseCase = mockk(relaxed = true)
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(moneyAccountStatusSupplier, router, sendFeedbackEmailUseCase, urlOpener, analytics)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN active account WHEN add funds clicked THEN options sheet opens with its deposit address`(
        model: AddFundsModel,
    ) = runTest {
        // Arrange
        val detailsModel = createModel(activeAccount(balance = model.balance))
        val openedSheets = detailsModel.bottomSheetNavigation.trackSlot()
        advanceUntilIdle()

        // Act
        detailsModel.uiState.value.onAddFundsClick()

        // Assert
        assertThat(openedSheets).containsExactly(
            MoneyAccountDetailsNavigation.AddFundsOptions(depositAddress = model.expectedDepositAddress),
        )
        verify(exactly = 1) { analytics.send(ofType<MoneyAccountAnalyticsEvents.AddFundsClicked>()) }
        detailsModel.onDestroy()
    }

    @Test
    fun `GIVEN options sheet WHEN receive clicked THEN options dismissed and receive sheet opens`() = runTest {
        // Arrange
        val model = createModel(activeAccount(balance = balance()))
        val openedSheets = model.bottomSheetNavigation.trackSlot()
        advanceUntilIdle()
        model.uiState.value.onAddFundsClick()

        // Act
        model.onReceiveClick(DEPOSIT_ADDRESS)

        // Assert
        assertThat(openedSheets).containsExactly(
            MoneyAccountDetailsNavigation.AddFundsOptions(depositAddress = DEPOSIT_ADDRESS),
            null,
            MoneyAccountDetailsNavigation.Receive(receiveConfig()),
        ).inOrder()
        model.onDestroy()
    }

    @Test
    fun `GIVEN options sheet WHEN dismissed THEN slot cleared`() = runTest {
        // Arrange
        val model = createModel(activeAccount(balance = balance()))
        val openedSheets = model.bottomSheetNavigation.trackSlot()
        advanceUntilIdle()
        model.uiState.value.onAddFundsClick()

        // Act
        model.onDismissAddFunds()

        // Assert
        assertThat(openedSheets.last()).isNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN receive opened from options WHEN options dismissal arrives late THEN receive sheet stays`() = runTest {
        // Arrange
        val model = createModel(activeAccount(balance = balance()))
        val openedSheets = model.bottomSheetNavigation.trackSlot()
        advanceUntilIdle()
        model.uiState.value.onAddFundsClick()
        model.onReceiveClick(DEPOSIT_ADDRESS)

        // Act
        model.onDismissAddFunds()

        // Assert
        assertThat(openedSheets.last()).isEqualTo(MoneyAccountDetailsNavigation.Receive(receiveConfig()))
        model.onDestroy()
    }

    private fun TestScope.createModel(status: MoneyAccountStatusValue): MoneyAccountDetailsModel {
        every { moneyAccountStatusSupplier(userWalletId) } returns flowOf(
            AccountStatus.Money(account = Account.Money(userWalletId = userWalletId), value = status),
        )
        return MoneyAccountDetailsModel(
            paramsContainer = MutableParamsContainer(MoneyAccountDetailsComponent.Params(userWalletId = userWalletId)),
            moneyAccountStatusSupplier = moneyAccountStatusSupplier,
            dispatchers = createTestingCoroutineDispatcherProvider(),
            router = router,
            sendFeedbackEmailUseCase = sendFeedbackEmailUseCase,
            urlOpener = urlOpener,
            getExploreUrlUseCase = getExploreUrlUseCase,
            analytics = analytics,
        )
    }

    private fun provideTestModels() = listOf(
        AddFundsModel(balance = balance(), expectedDepositAddress = DEPOSIT_ADDRESS),
        AddFundsModel(balance = null, expectedDepositAddress = null),
    )

    data class AddFundsModel(val balance: MoneyAccountStatusValue.Balance?, val expectedDepositAddress: String?)

    private fun activeAccount(balance: MoneyAccountStatusValue.Balance?) = MoneyAccountStatusValue.Account.Active(
        source = StatusSource.ACTUAL,
        cryptoCurrency = token,
        balance = balance,
        customerId = CUSTOMER_ID,
    )

    private fun balance() = MoneyAccountStatusValue.Balance(
        cryptoAmount = BigDecimal("25250.50"),
        depositAddress = DEPOSIT_ADDRESS,
        currencyCode = "USD",
        fiatRate = BigDecimal.ONE,
    )

    private fun receiveConfig() = TokenReceiveConfig(
        shouldShowWarning = true,
        cryptoCurrency = token,
        userWalletId = userWalletId,
        shouldShowMemoDisclaimer = false,
        receiveAddress = listOf(
            ReceiveAddressModel(displayType = ReceiveAddressModel.DisplayType.Default, value = DEPOSIT_ADDRESS),
        ),
    )

    private fun SlotNavigation<MoneyAccountDetailsNavigation>.trackSlot(): List<MoneyAccountDetailsNavigation?> {
        val tracked = mutableListOf<MoneyAccountDetailsNavigation?>()
        subscribe { event -> tracked.add(event.transformer(tracked.lastOrNull())) }
        return tracked
    }

    private companion object {
        const val CUSTOMER_ID = "customer"
        const val DEPOSIT_ADDRESS = "0xdeposit"
    }
}