package com.tangem.features.send.subcomponents.notifications.model

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.routing.AppRouter
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.account.status.usecase.GetAccountCurrencyByAddressUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.notifications.GetTronFeeNotificationShowCountUseCase
import com.tangem.domain.notifications.IncrementNotificationsShowCountUseCase
import com.tangem.domain.tokens.GetAssetRequirementsUseCase
import com.tangem.domain.tokens.GetBalanceNotEnoughForFeeWarningUseCase
import com.tangem.domain.tokens.GetCurrencyCheckUseCase
import com.tangem.domain.tokens.IsAmountSubtractAvailableUseCase
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyCheck
import com.tangem.domain.transaction.usecase.ValidateTransactionUseCase
import com.tangem.features.send.api.subcomponents.notifications.SendNotificationsComponent
import com.tangem.features.send.api.subcomponents.notifications.SendNotificationsComponent.Params.NotificationData
import com.tangem.features.send.api.subcomponents.notifications.SendNotificationsUpdateListener
import com.tangem.features.send.api.subcomponents.notifications.SendNotificationsUpdateTrigger
import com.tangem.features.send.loadedStatus
import com.tangem.features.send.testDispatcherProvider
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal

/**
 * Covers who reports a Tron gasless compensation the balance can't cover when the fee token IS the sent
 * token ([REDACTED_TASK_KEY]). [GetBalanceNotEnoughForFeeWarningUseCase] deliberately stays silent there — the
 * amount-subtraction path owns the case — so the insufficiency has to surface here instead, either as a
 * reduced amount ([NotificationUM.Warning.FeeCoverageNotification]) or, when even the fee alone doesn't
 * fit, as [NotificationUM.Error.TotalExceedsBalance].
 *
 * Also covers when the informational Tron network-fee notice joins the list: never for a gasless fee, and
 * at most [TRON_FEE_NOTICE_CAP] times overall.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class NotificationsModelTest {

    private val appRouter: AppRouter = mockk(relaxed = true)
    private val isAmountSubtractAvailableUseCase: IsAmountSubtractAvailableUseCase = mockk(relaxed = true)
    private val getCurrencyCheckUseCase: GetCurrencyCheckUseCase = mockk(relaxed = true)
    private val getBalanceNotEnoughForFeeWarningUseCase: GetBalanceNotEnoughForFeeWarningUseCase =
        mockk(relaxed = true)
    private val validateTransactionUseCase: ValidateTransactionUseCase = mockk(relaxed = true)
    private val getTronFeeNotificationShowCountUseCase: GetTronFeeNotificationShowCountUseCase = mockk(relaxed = true)
    private val incrementNotificationsShowCountUseCase: IncrementNotificationsShowCountUseCase = mockk(relaxed = true)
    private val getAssetRequirementsUseCase: GetAssetRequirementsUseCase = mockk(relaxed = true)
    private val getAccountCurrencyByAddressUseCase: GetAccountCurrencyByAddressUseCase = mockk(relaxed = true)
    private val notificationsUpdateTrigger: SendNotificationsUpdateTrigger = mockk(relaxed = true)
    private val notificationsUpdateListener: SendNotificationsUpdateListener = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)

    private val userWalletId = UserWalletId("1234567890ABCDEF")
    private val tronUsdt: CryptoCurrency.Token = MockCryptoCurrencyFactory().createToken(
        blockchain = Blockchain.Tron,
        id = "tether",
        contractAddress = "TUsdt",
    )

    @BeforeEach
    fun setUp() {
        clearMocks(
            appRouter,
            isAmountSubtractAvailableUseCase,
            getCurrencyCheckUseCase,
            getBalanceNotEnoughForFeeWarningUseCase,
            validateTransactionUseCase,
            getTronFeeNotificationShowCountUseCase,
            incrementNotificationsShowCountUseCase,
            getAssetRequirementsUseCase,
            getAccountCurrencyByAddressUseCase,
            notificationsUpdateTrigger,
            notificationsUpdateListener,
            analyticsEventHandler,
        )
        every { notificationsUpdateListener.updateTriggerFlow } returns emptyFlow()
        coEvery { getCurrencyCheckUseCase(any(), any(), any(), any(), any(), any(), any()) } returns emptyCheck()
        // What the real use case returns for a same-token Tron gasless fee — see
        // GetBalanceNotEnoughForFeeWarningUseCaseTest. Nothing else may cover the insufficiency.
        coEvery {
            getBalanceNotEnoughForFeeWarningUseCase(any(), any(), any(), any())
        } returns null.right()
        // The sent token is a Tron token, so the informational fee notice would join every list; exhaust the
        // counter to keep the assertions about the balance notifications only.
        coEvery { getTronFeeNotificationShowCountUseCase() } returns TRON_FEE_NOTICE_CAP
        // The pair handed to the use case is what decides this; IsAmountSubtractAvailableUseCaseTest covers
        // that a same-token gasless fee resolves to true.
        coEvery { isAmountSubtractAvailableUseCase(any(), any(), any()) } returns true.right()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun sameTokenGaslessFee(model: TestModel) = runTest {
        // Arrange
        val sut = createModel(testScope = this, balance = model.balance, enteredAmount = model.enteredAmount)

        // Act
        advanceUntilIdle()
        val notifications = sut.uiState.value

        // Assert
        assertThat(notifications.contains(NotificationUM.Error.TotalExceedsBalance))
            .isEqualTo(model.expectsTotalExceedsBalance)
        assertThat(notifications.any { it is NotificationUM.Warning.FeeCoverageNotification })
            .isEqualTo(model.expectsFeeCoverage)
        sut.onDestroy()
    }

    internal data class TestModel(
        val balance: BigDecimal,
        val enteredAmount: BigDecimal,
        val expectsTotalExceedsBalance: Boolean,
        val expectsFeeCoverage: Boolean,
    )

    private fun provideTestModels() = listOf(
        // Max send — the whole balance is entered and the compensation comes out of it. This is the case
        // [REDACTED_TASK_KEY] fixed: the amount is reduced to balance - fee, no error.
        TestModel(
            balance = BigDecimal("2.80"),
            enteredAmount = BigDecimal("2.80"),
            expectsTotalExceedsBalance = false,
            expectsFeeCoverage = true,
        ),
        // Partial send whose amount + compensation overshoots: same reduction, still no error.
        TestModel(
            balance = BigDecimal("2.80"),
            enteredAmount = BigDecimal("0.15"),
            expectsTotalExceedsBalance = false,
            expectsFeeCoverage = true,
        ),
        // The compensation alone doesn't fit, so there is nothing left to reduce.
        TestModel(
            balance = BigDecimal("1.00"),
            enteredAmount = BigDecimal("1.00"),
            expectsTotalExceedsBalance = true,
            expectsFeeCoverage = false,
        ),
        // Balance exactly equals the compensation: sending anything at all is impossible.
        TestModel(
            balance = TRON_GASLESS_FEE,
            enteredAmount = TRON_GASLESS_FEE,
            expectsTotalExceedsBalance = true,
            expectsFeeCoverage = false,
        ),
        // More than the balance is entered — reduction doesn't apply, the total is simply too big.
        TestModel(
            balance = BigDecimal("2.80"),
            enteredAmount = BigDecimal("3.00"),
            expectsTotalExceedsBalance = true,
            expectsFeeCoverage = false,
        ),
        // Amount and compensation both fit — neither notification belongs.
        TestModel(
            balance = BigDecimal("100"),
            enteredAmount = BigDecimal("1"),
            expectsTotalExceedsBalance = false,
            expectsFeeCoverage = false,
        ),
    )

    private fun createModel(
        testScope: TestScope,
        balance: BigDecimal,
        enteredAmount: BigDecimal,
        fee: Fee = tronGaslessFee(),
    ): NotificationsModel {
        // The fee token IS the sent token, so both statuses are the same snapshot.
        val status = loadedStatus(currency = tronUsdt, balance = balance)
        val params = SendNotificationsComponent.Params(
            analyticsCategoryName = "test_send",
            userWalletId = userWalletId,
            cryptoCurrencyStatus = status,
            appCurrency = AppCurrency.Default,
            notificationData = NotificationData(
                destinationAddress = null,
                memo = null,
                amountValue = enteredAmount,
                reduceAmountBy = BigDecimal.ZERO,
                isIgnoreReduce = false,
                fee = fee,
                feeError = null,
                feeCryptoCurrencyStatus = status,
            ),
            callback = mockk(relaxed = true),
        )
        return NotificationsModel(
            paramsContainer = MutableParamsContainer(params),
            dispatchers = testScope.testDispatcherProvider(),
            appRouter = appRouter,
            isAmountSubtractAvailableUseCase = isAmountSubtractAvailableUseCase,
            getCurrencyCheckUseCase = getCurrencyCheckUseCase,
            getBalanceNotEnoughForFeeWarningUseCase = getBalanceNotEnoughForFeeWarningUseCase,
            validateTransactionUseCase = validateTransactionUseCase,
            getTronFeeNotificationShowCountUseCase = getTronFeeNotificationShowCountUseCase,
            incrementNotificationsShowCountUseCase = incrementNotificationsShowCountUseCase,
            getAssetRequirementsUseCase = getAssetRequirementsUseCase,
            getAccountCurrencyByAddressUseCase = getAccountCurrencyByAddressUseCase,
            notificationsUpdateTrigger = notificationsUpdateTrigger,
            notificationsUpdateListener = notificationsUpdateListener,
            analyticsEventHandler = analyticsEventHandler,
        )
    }

    @Test
    fun `GIVEN gasless fee WHEN notifications built THEN Tron fee notice is hidden`() = runTest {
        // Arrange
        coEvery { getTronFeeNotificationShowCountUseCase() } returns 0
        val sut = createModel(
            testScope = this,
            balance = BigDecimal("100"),
            enteredAmount = BigDecimal.ONE,
            fee = tronGaslessFee(),
        )

        // Act
        advanceUntilIdle()

        // Assert
        assertThat(sut.uiState.value.filterIsInstance<NotificationUM.Info>()).isEmpty()
        coVerify(exactly = 0) { incrementNotificationsShowCountUseCase(any()) }
        sut.onDestroy()
    }

    @Test
    fun `GIVEN coin fee WHEN notifications built THEN Tron fee notice is shown and counted once`() = runTest {
        // Arrange
        coEvery { getTronFeeNotificationShowCountUseCase() } returns 0
        val sut = createModel(
            testScope = this,
            balance = BigDecimal("100"),
            enteredAmount = BigDecimal.ONE,
            fee = trxFee(),
        )

        // Act
        advanceUntilIdle()

        // Assert
        assertThat(sut.uiState.value.filterIsInstance<NotificationUM.Info>()).hasSize(1)
        coVerify(exactly = 1) { incrementNotificationsShowCountUseCase(tronUsdt) }
        sut.onDestroy()
    }

    @ParameterizedTest
    @ValueSource(ints = [TRON_FEE_NOTICE_CAP, TRON_FEE_NOTICE_CAP + 1])
    fun `GIVEN show count at cap WHEN notifications built THEN Tron fee notice is hidden`(showCount: Int) = runTest {
        // Arrange
        coEvery { getTronFeeNotificationShowCountUseCase() } returns showCount
        val sut = createModel(
            testScope = this,
            balance = BigDecimal("100"),
            enteredAmount = BigDecimal.ONE,
            fee = trxFee(),
        )

        // Act
        advanceUntilIdle()

        // Assert
        assertThat(sut.uiState.value.filterIsInstance<NotificationUM.Info>()).isEmpty()
        coVerify(exactly = 0) { incrementNotificationsShowCountUseCase(any()) }
        sut.onDestroy()
    }

    @Test
    fun `GIVEN last allowed show WHEN notifications built THEN Tron fee notice is shown`() = runTest {
        // Arrange
        coEvery { getTronFeeNotificationShowCountUseCase() } returns TRON_FEE_NOTICE_CAP - 1
        val sut = createModel(
            testScope = this,
            balance = BigDecimal("100"),
            enteredAmount = BigDecimal.ONE,
            fee = trxFee(),
        )

        // Act
        advanceUntilIdle()

        // Assert
        assertThat(sut.uiState.value.filterIsInstance<NotificationUM.Info>()).hasSize(1)
        sut.onDestroy()
    }

    /** Tron gasless denominates the compensation in a token but ships it as a plain [Fee.Common]. */
    private fun tronGaslessFee(): Fee = Fee.Common(
        Amount(
            token = Token(
                symbol = tronUsdt.symbol,
                contractAddress = tronUsdt.contractAddress,
                decimals = tronUsdt.decimals,
            ),
            value = TRON_GASLESS_FEE,
        ),
    )

    private fun trxFee(): Fee = Fee.Common(
        Amount(value = BigDecimal("5"), blockchain = Blockchain.Tron),
    )

    private fun emptyCheck() = CryptoCurrencyCheck(
        dustValue = null,
        reserveAmount = null,
        minimumSendAmount = null,
        existentialDeposit = null,
        utxoAmountLimit = null,
        isAccountFunded = true,
        rentWarning = null,
    )

    private companion object {
        val TRON_GASLESS_FEE: BigDecimal = BigDecimal("2.74")
        const val TRON_FEE_NOTICE_CAP = 3
    }
}