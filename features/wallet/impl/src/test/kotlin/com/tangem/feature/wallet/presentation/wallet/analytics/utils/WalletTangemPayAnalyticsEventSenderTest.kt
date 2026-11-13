package com.tangem.feature.wallet.presentation.wallet.analytics.utils

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.AccountType
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.NoticeShown
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.Placement
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletUM
import com.tangem.feature.wallet.presentation.wallet.utils.ScreenLifecycleProvider
import com.tangem.test.mock.MockAccounts
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@OptIn(ExperimentalCoroutinesApi::class)
internal class WalletTangemPayAnalyticsEventSenderTest {

    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val screenLifecycleProvider = ScreenLifecycleProvider()
    private val stateController = WalletStateController()
    private val selectionScope = CoroutineScope(UnconfinedTestDispatcher())

    private val sender = WalletTangemPayAnalyticsEventSender(
        analyticsEventHandler = analyticsEventHandler,
        screenLifecycleProvider = screenLifecycleProvider,
        stateController = stateController,
    )

    @BeforeEach
    fun setUp() {
        sender.observeSelectedWallet(selectionScope)
        selectWallet(WALLET_ID)
    }

    @AfterEach
    fun tearDown() {
        selectionScope.cancel()
    }

    @Test
    fun `GIVEN payment loaded with flag WHEN send THEN notice shown once for home row`() {
        // Act
        sender.send(paymentStatus(isKycAdditionalInfoRequired = true))
        sender.send(paymentStatus(isKycAdditionalInfoRequired = true))

        // Assert
        verifyNoticeSent(accountType = AccountType.PAYMENT, times = 1)
    }

    @Test
    fun `GIVEN money active with flag WHEN send THEN notice shown once for home row`() {
        // Act
        sender.send(moneyStatus(isKycAdditionalInfoRequired = true))
        sender.send(moneyStatus(isKycAdditionalInfoRequired = true))

        // Assert
        verifyNoticeSent(accountType = AccountType.MONEY, times = 1)
    }

    @Test
    fun `GIVEN statuses without flag WHEN send THEN no notice`() {
        // Act
        sender.send(paymentStatus(isKycAdditionalInfoRequired = false))
        sender.send(moneyStatus(isKycAdditionalInfoRequired = false))

        // Assert
        verify(exactly = 0) { analyticsEventHandler.send(ofType<NoticeShown>()) }
    }

    @Test
    fun `GIVEN payment account issuing with flag WHEN send THEN no notice`() {
        // Act
        sender.send(paymentStatus(isKycAdditionalInfoRequired = true, isPaymentAccountIssuing = true))

        // Assert
        verify(exactly = 0) { analyticsEventHandler.send(ofType<NoticeShown>()) }
    }

    @Test
    fun `GIVEN flag dropped WHEN flag returns THEN notice sent again`() {
        // Arrange
        sender.send(paymentStatus(isKycAdditionalInfoRequired = true))
        sender.send(paymentStatus(isKycAdditionalInfoRequired = false))

        // Act
        sender.send(paymentStatus(isKycAdditionalInfoRequired = true))

        // Assert
        verifyNoticeSent(accountType = AccountType.PAYMENT, times = 2)
    }

    @ParameterizedTest
    @MethodSource("transientPaymentValues")
    fun `GIVEN payment notice sent WHEN transient status and flag again THEN notice not repeated`(
        transient: PaymentAccountStatusValue,
    ) {
        // Arrange
        sender.send(paymentStatus(isKycAdditionalInfoRequired = true))
        sender.send(AccountStatus.Payment(account = Account.Payment(WALLET_ID), value = transient))

        // Act
        sender.send(paymentStatus(isKycAdditionalInfoRequired = true))

        // Assert
        verifyNoticeSent(accountType = AccountType.PAYMENT, times = 1)
    }

    @ParameterizedTest
    @MethodSource("transientMoneyValues")
    fun `GIVEN money notice sent WHEN transient status and flag again THEN notice not repeated`(
        transient: MoneyAccountStatusValue,
    ) {
        // Arrange
        sender.send(moneyStatus(isKycAdditionalInfoRequired = true))
        sender.send(AccountStatus.Money(account = Account.Money(WALLET_ID), value = transient))

        // Act
        sender.send(moneyStatus(isKycAdditionalInfoRequired = true))

        // Assert
        verifyNoticeSent(accountType = AccountType.MONEY, times = 1)
    }

    @Test
    fun `GIVEN payment notice sent WHEN card issue failed and flag again THEN notice sent again`() {
        // Arrange
        sender.send(paymentStatus(isKycAdditionalInfoRequired = true))
        sender.send(
            AccountStatus.Payment(
                account = Account.Payment(WALLET_ID),
                value = PaymentAccountStatusValue.Error.CardIssueFailed(customerId = "customer"),
            ),
        )

        // Act
        sender.send(paymentStatus(isKycAdditionalInfoRequired = true))

        // Assert
        verifyNoticeSent(accountType = AccountType.PAYMENT, times = 2)
    }

    @Test
    fun `GIVEN background WHEN send with flag THEN nothing sent until resume`() {
        // Arrange
        screenLifecycleProvider.onPause()

        // Act
        sender.send(paymentStatus(isKycAdditionalInfoRequired = true))
        sender.send(moneyStatus(isKycAdditionalInfoRequired = true))

        // Assert
        verify(exactly = 0) { analyticsEventHandler.send(ofType<NoticeShown>()) }
    }

    @Test
    fun `GIVEN flag received in background WHEN resumed THEN notice sent once`() {
        // Arrange
        screenLifecycleProvider.onPause()
        sender.send(paymentStatus(isKycAdditionalInfoRequired = true))

        // Act
        resume()

        // Assert
        verifyNoticeSent(accountType = AccountType.PAYMENT, times = 1)
    }

    @Test
    fun `GIVEN notice sent WHEN resume without leaving THEN not repeated`() {
        // Arrange
        sender.send(paymentStatus(isKycAdditionalInfoRequired = true))
        screenLifecycleProvider.onPause()
        sender.send(paymentStatus(isKycAdditionalInfoRequired = true, customerId = "refreshed"))

        // Act
        resume()

        // Assert
        verifyNoticeSent(accountType = AccountType.PAYMENT, times = 1)
    }

    @Test
    fun `GIVEN notice sent WHEN account opened and resumed THEN notice sent again`() {
        // Arrange
        sender.send(paymentStatus(isKycAdditionalInfoRequired = true))
        sender.onAccountOpened(userWalletId = WALLET_ID, accountType = AccountType.PAYMENT)
        screenLifecycleProvider.onPause()

        // Act
        resume()

        // Assert
        verifyNoticeSent(accountType = AccountType.PAYMENT, times = 2)
    }

    @Test
    fun `GIVEN notice sent WHEN another account opened and resumed THEN not repeated`() {
        // Arrange
        sender.send(paymentStatus(isKycAdditionalInfoRequired = true))
        sender.onAccountOpened(userWalletId = WALLET_ID, accountType = AccountType.MONEY)
        screenLifecycleProvider.onPause()

        // Act
        resume()

        // Assert
        verifyNoticeSent(accountType = AccountType.PAYMENT, times = 1)
    }

    @Test
    fun `GIVEN flag dropped in background WHEN flag returns after resume THEN notice sent again`() {
        // Arrange
        sender.send(moneyStatus(isKycAdditionalInfoRequired = true))
        screenLifecycleProvider.onPause()
        sender.send(moneyStatus(isKycAdditionalInfoRequired = false))
        resume()

        // Act
        sender.send(moneyStatus(isKycAdditionalInfoRequired = true))

        // Assert
        verifyNoticeSent(accountType = AccountType.MONEY, times = 2)
    }

    @Test
    fun `GIVEN two wallets with flag WHEN each one gets selected THEN notice sent per wallet`() {
        // Arrange
        sender.send(paymentStatus(isKycAdditionalInfoRequired = true, userWalletId = WALLET_ID))
        sender.send(paymentStatus(isKycAdditionalInfoRequired = true, userWalletId = OTHER_WALLET_ID))

        // Act
        selectWallet(OTHER_WALLET_ID)

        // Assert
        verifyNoticeSent(accountType = AccountType.PAYMENT, times = 2)
    }

    @Test
    fun `GIVEN flag on non-selected wallet WHEN send THEN no notice`() {
        // Act
        sender.send(paymentStatus(isKycAdditionalInfoRequired = true, userWalletId = OTHER_WALLET_ID))
        sender.send(moneyStatus(isKycAdditionalInfoRequired = true, userWalletId = OTHER_WALLET_ID))

        // Assert
        verify(exactly = 0) { analyticsEventHandler.send(ofType<NoticeShown>()) }
    }

    @Test
    fun `GIVEN flag on wallet A WHEN switching A to B to A THEN notice for A sent twice`() {
        // Arrange
        sender.send(moneyStatus(isKycAdditionalInfoRequired = true, userWalletId = WALLET_ID))
        sender.send(moneyStatus(isKycAdditionalInfoRequired = false, userWalletId = OTHER_WALLET_ID))

        // Act
        selectWallet(OTHER_WALLET_ID)
        selectWallet(WALLET_ID)

        // Assert
        verifyNoticeSent(accountType = AccountType.MONEY, times = 2)
    }

    @Test
    fun `GIVEN pending flag on non-selected wallet WHEN onResume THEN no notice`() {
        // Arrange
        screenLifecycleProvider.onPause()
        sender.send(paymentStatus(isKycAdditionalInfoRequired = true, userWalletId = OTHER_WALLET_ID))

        // Act
        resume()

        // Assert
        verify(exactly = 0) { analyticsEventHandler.send(ofType<NoticeShown>()) }
    }

    @Test
    fun `GIVEN wallet with flag selected in background WHEN resumed THEN notice sent once`() {
        // Arrange
        sender.send(paymentStatus(isKycAdditionalInfoRequired = true, userWalletId = OTHER_WALLET_ID))
        screenLifecycleProvider.onPause()
        selectWallet(OTHER_WALLET_ID)

        // Act
        resume()

        // Assert
        verifyNoticeSent(accountType = AccountType.PAYMENT, times = 1)
    }

    @Test
    fun `GIVEN flag before first selection WHEN wallet selected THEN notice sent once`() {
        // Arrange
        val initialStateController = WalletStateController()
        val initialSender = WalletTangemPayAnalyticsEventSender(
            analyticsEventHandler = analyticsEventHandler,
            screenLifecycleProvider = screenLifecycleProvider,
            stateController = initialStateController,
        )
        initialSender.observeSelectedWallet(selectionScope)
        initialSender.send(paymentStatus(isKycAdditionalInfoRequired = true))

        // Act
        selectWallet(WALLET_ID, controller = initialStateController)

        // Assert
        verifyNoticeSent(accountType = AccountType.PAYMENT, times = 1)
    }

    private fun selectWallet(userWalletId: UserWalletId, controller: WalletStateController = stateController) {
        controller.update { state ->
            state.copy(
                selectedWalletIndex = ALL_WALLET_IDS.indexOf(userWalletId),
                wallets2 = ALL_WALLET_IDS.map(::walletUM).toPersistentList(),
            )
        }
    }

    private fun walletUM(userWalletId: UserWalletId): WalletUM = mockk {
        every { walletsBalanceUM.id } returns userWalletId
    }

    private fun resume() {
        screenLifecycleProvider.onResume()
        sender.onResume()
    }

    private fun verifyNoticeSent(accountType: AccountType, times: Int) {
        verify(exactly = times) {
            analyticsEventHandler.send(NoticeShown(accountType = accountType, placement = Placement.HOME_ROW))
        }
    }

    private fun paymentStatus(
        isKycAdditionalInfoRequired: Boolean,
        isPaymentAccountIssuing: Boolean = false,
        customerId: String = "customer",
        userWalletId: UserWalletId = WALLET_ID,
    ): AccountStatus.Payment = MockAccounts.createPaymentAccountStatus(
        userWalletId = userWalletId,
        customerId = customerId,
        isPaymentAccountIssuing = isPaymentAccountIssuing,
        isKycAdditionalInfoRequired = isKycAdditionalInfoRequired,
    )

    private fun moneyStatus(
        isKycAdditionalInfoRequired: Boolean,
        userWalletId: UserWalletId = WALLET_ID,
    ): AccountStatus.Money {
        return AccountStatus.Money(
            account = Account.Money(userWalletId),
            value = MoneyAccountStatusValue.Account.Active(
                source = StatusSource.ACTUAL,
                cryptoCurrency = MockAccounts.createPaymentAccountToken(),
                balance = null,
                customerId = "customer",
                productInstanceId = "instance",
                error = null,
                isBankOfframpAvailable = null,
                isKycAdditionalInfoRequired = isKycAdditionalInfoRequired,
            ),
        )
    }

    private companion object {
        val WALLET_ID = UserWalletId("01")
        val OTHER_WALLET_ID = UserWalletId("02")
        val ALL_WALLET_IDS = listOf(WALLET_ID, OTHER_WALLET_ID)

        @JvmStatic
        fun transientPaymentValues(): List<PaymentAccountStatusValue> = listOf(
            PaymentAccountStatusValue.Loading,
            PaymentAccountStatusValue.Error.Unavailable,
            PaymentAccountStatusValue.Error.NotSynced,
            PaymentAccountStatusValue.Error.ExposedDevice,
        )

        @JvmStatic
        fun transientMoneyValues(): List<MoneyAccountStatusValue> = listOf(
            MoneyAccountStatusValue.Error.Unavailable,
            MoneyAccountStatusValue.Error.NotSynced,
            MoneyAccountStatusValue.Error.ExposedDevice,
        )
    }
}