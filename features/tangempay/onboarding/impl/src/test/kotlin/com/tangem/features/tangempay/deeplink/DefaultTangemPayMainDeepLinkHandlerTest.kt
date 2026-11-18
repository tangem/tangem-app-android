package com.tangem.features.tangempay.deeplink

import android.net.Uri
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.deeplink.DeeplinkConst.CUSTOMER_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.CUSTOMER_WALLET_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TYPE_KEY
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.deeplink.DeepLinkRequest
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.tangempay.TangemPayDetailsInitialRoute
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusFetcher
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusSupplier
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoPushOpenedEvent
import com.tangem.domain.tangempay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.tangempay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.wallets.models.errors.GetUserWalletError
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.SelectWalletUseCase
import com.tangem.features.wallet.deeplink.WalletDeepLinkActionTrigger
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.MockAccounts
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultTangemPayMainDeepLinkHandlerTest {

    private val router: Router = mockk(relaxed = true)
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val selectWalletUseCase: SelectWalletUseCase = mockk()
    private val walletDeepLinkActionTrigger: WalletDeepLinkActionTrigger = mockk(relaxed = true)
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher = mockk(relaxed = true)
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier = mockk()
    private val moneyAccountStatusFetcher: MoneyAccountStatusFetcher = mockk(relaxed = true)
    private val moneyAccountStatusSupplier: MoneyAccountStatusSupplier = mockk()
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)

    private val unlockedWallet: UserWallet = mockk<UserWallet.Hot>(relaxed = true) {
        every { walletId } returns USER_WALLET_ID
        every { isLocked } returns false
    }

    private val lockedWallet: UserWallet = mockk<UserWallet.Hot>(relaxed = true) {
        every { walletId } returns USER_WALLET_ID
        every { isLocked } returns true
    }

    private val handler = DefaultTangemPayMainDeepLinkHandler(
        getUserWalletUseCase = getUserWalletUseCase,
        selectWalletUseCase = selectWalletUseCase,
        walletDeepLinkActionTrigger = walletDeepLinkActionTrigger,
        paymentAccountStatusFetcher = paymentAccountStatusFetcher,
        paymentAccountSupplier = paymentAccountStatusSupplier,
        moneyAccountStatusFetcher = moneyAccountStatusFetcher,
        moneyAccountStatusSupplier = moneyAccountStatusSupplier,
        analyticsEventHandler = analyticsEventHandler,
    )

    @BeforeEach
    fun setUp() {
        clearMocks(
            router,
            getUserWalletUseCase,
            selectWalletUseCase,
            walletDeepLinkActionTrigger,
            paymentAccountStatusFetcher,
            paymentAccountStatusSupplier,
            moneyAccountStatusFetcher,
            moneyAccountStatusSupplier,
            analyticsEventHandler,
        )
        every { router.popTo(any<AppRoute>(), any()) } answers { secondArg<(Boolean) -> Unit>().invoke(true) }
        every { getUserWalletUseCase(any()) } returns unlockedWallet.right()
        coEvery { selectWalletUseCase(any()) } returns unlockedWallet.right()
        every { paymentAccountStatusSupplier(any<UserWalletId>()) } returns flowOf(ACTIVE_PAYMENT)
        every { moneyAccountStatusSupplier(any<UserWalletId>()) } returns emptyFlow()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN kyc push WHEN handled THEN opens account by priority`(model: ResolveAccountModel) = runTest {
        // Arrange
        val pushedRoutes = mutableListOf<Route>()
        every { router.push(capture(pushedRoutes), any()) } just Runs
        every { paymentAccountStatusSupplier(any<UserWalletId>()) } returns flowOf(model.payment)
        every { moneyAccountStatusSupplier(any<UserWalletId>()) } returns (model.money?.let(::flowOf) ?: emptyFlow())

        // Act
        handle(scope = this, queryParams = kycPushParams())
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { router.popTo(route = AppRoute.Wallet, onComplete = any()) }
        verify(exactly = 1) { walletDeepLinkActionTrigger.selectWallet(USER_WALLET_ID) }
        assertThat(pushedRoutes).isEqualTo(listOfNotNull(model.expectedRoute))
    }

    @Test
    fun `GIVEN payment account opens WHEN kyc push handled THEN money status not requested`() = runTest {
        // Arrange
        every { paymentAccountStatusSupplier(any<UserWalletId>()) } returns flowOf(ACTIVE_PAYMENT)

        // Act
        handle(scope = this, queryParams = kycPushParams())
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { moneyAccountStatusFetcher(any<MoneyAccountStatusFetcher.Params>()) }
        verify(exactly = 0) { moneyAccountStatusSupplier(any<UserWalletId>()) }
    }

    @Test
    fun `GIVEN payment account is issuing WHEN kyc push handled THEN money status refreshed before it is read`() =
        runTest {
            // Arrange
            every { paymentAccountStatusSupplier(any<UserWalletId>()) } returns flowOf(ISSUING_PAYMENT)

            // Act
            handle(scope = this, queryParams = kycPushParams())
            advanceUntilIdle()

            // Assert
            coVerifyOrder {
                moneyAccountStatusFetcher(MoneyAccountStatusFetcher.Params(USER_WALLET_ID))
                moneyAccountStatusSupplier(USER_WALLET_ID)
            }
        }

    @Test
    fun `GIVEN kyc push WHEN handled THEN push opened event sent once`() = runTest {
        // Act
        handle(scope = this, queryParams = kycPushParams())
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { analyticsEventHandler.send(TangemPayKycAdditionalInfoPushOpenedEvent()) }
        verify(exactly = 1) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN kyc push for unknown wallet WHEN handled THEN push opened event sent AND no account opened`() = runTest {
        // Arrange
        every { getUserWalletUseCase(any()) } returns GetUserWalletError.UserWalletNotFound.left()

        // Act
        handle(scope = this, queryParams = kycPushParams())
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { analyticsEventHandler.send(TangemPayKycAdditionalInfoPushOpenedEvent()) }
        verify(exactly = 1) { router.popTo(route = AppRoute.Wallet, onComplete = any()) }
        verify(exactly = 0) { router.push(any(), any()) }
    }

    @Test
    fun `GIVEN kyc push for locked wallet WHEN handled THEN push opened event sent AND no account opened`() = runTest {
        // Arrange
        every { getUserWalletUseCase(any()) } returns lockedWallet.right()

        // Act
        handle(scope = this, queryParams = kycPushParams())
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { analyticsEventHandler.send(TangemPayKycAdditionalInfoPushOpenedEvent()) }
        verify(exactly = 1) { router.popTo(route = AppRoute.Wallet, onComplete = any()) }
        verify(exactly = 0) { router.push(any(), any()) }
    }

    @Test
    fun `GIVEN card_ready push WHEN handled THEN push opened event not sent`() = runTest {
        // Arrange
        val queryParams = mapOf(TYPE_KEY to "card_ready", CUSTOMER_WALLET_ID_KEY to USER_WALLET_ID.stringValue)

        // Act
        handle(scope = this, queryParams = queryParams)
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }

    private fun provideTestModels() = listOf(
        ResolveAccountModel(
            case = "active payment",
            payment = ACTIVE_PAYMENT,
            money = null,
            expectedRoute = paymentRoute(ACTIVE_PAYMENT),
        ),
        ResolveAccountModel(
            case = "issuing payment, active money",
            payment = ISSUING_PAYMENT,
            money = ACTIVE_MONEY,
            expectedRoute = MONEY_ROUTE,
        ),
        ResolveAccountModel(
            case = "payment not created, active money",
            payment = NOT_CREATED_PAYMENT,
            money = ACTIVE_MONEY,
            expectedRoute = MONEY_ROUTE,
        ),
        ResolveAccountModel(
            case = "issuing payment, empty money",
            payment = ISSUING_PAYMENT,
            money = EMPTY_MONEY,
            expectedRoute = paymentRoute(ISSUING_PAYMENT),
        ),
        ResolveAccountModel(
            case = "payment not created, money never emitted",
            payment = NOT_CREATED_PAYMENT,
            money = null,
            expectedRoute = null,
        ),
    )

    private fun handle(scope: TestScope, queryParams: Map<String, String>) = handler.handle(
        DeepLinkRequest(
            uri = mockk<Uri>(),
            queryParams = queryParams,
            scope = scope,
            isFromOnNewIntent = false,
            router = router,
        ),
    )

    private fun kycPushParams(): Map<String, String> = mapOf(
        TYPE_KEY to "kyc_additional_info",
        CUSTOMER_ID_KEY to "c1",
        CUSTOMER_WALLET_ID_KEY to USER_WALLET_ID.stringValue,
    )

    internal data class ResolveAccountModel(
        val case: String,
        val payment: AccountStatus.Payment,
        val money: AccountStatus.Money?,
        val expectedRoute: AppRoute?,
    ) {
        override fun toString(): String = case
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("0A1B2C")

        val ACTIVE_PAYMENT = MockAccounts.createPaymentAccountStatus(
            userWalletId = USER_WALLET_ID,
            isPaymentAccountIssuing = false,
        )

        val ISSUING_PAYMENT = MockAccounts.createPaymentAccountStatus(
            userWalletId = USER_WALLET_ID,
            isPaymentAccountIssuing = true,
        )

        val NOT_CREATED_PAYMENT = AccountStatus.Payment(
            account = Account.Payment(USER_WALLET_ID),
            value = PaymentAccountStatusValue.NotCreated,
        )

        val ACTIVE_MONEY = AccountStatus.Money(
            account = Account.Money(USER_WALLET_ID),
            value = MoneyAccountStatusValue.Account.Active(
                source = StatusSource.ACTUAL,
                cryptoCurrency = MockAccounts.createPaymentAccountToken(),
                balance = null,
                customerId = "c1",
                productInstanceId = "product_1",
                error = null,
                isKycAdditionalInfoRequired = true,
            ),
        )

        val EMPTY_MONEY = AccountStatus.Money(
            account = Account.Money(USER_WALLET_ID),
            value = MoneyAccountStatusValue.Empty,
        )

        val MONEY_ROUTE = AppRoute.MoneyAccountDetails(userWalletId = USER_WALLET_ID, isOpenedFromKycPush = true)

        fun paymentRoute(status: AccountStatus.Payment) = AppRoute.TangemPayDetails(
            status = status,
            initialRoute = TangemPayDetailsInitialRoute.ACCOUNT_DETAILS,
            isOpenedFromKycPush = true,
        )
    }
}