package com.tangem.features.tangempay.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.pay.TangemPayDetailsInitialRoute
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.logging.TangemLogger
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultTangemPayAccountDeepLinkHandlerTest {

    private val appRouter: AppRouter = mockk(relaxed = true)
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher = mockk(relaxed = true)
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier = mockk()

    @BeforeEach
    fun setUp() {
        clearMocks(appRouter, userWalletsListRepository, paymentAccountStatusFetcher, paymentAccountStatusSupplier)
        mockkObject(TangemLogger)
        every { TangemLogger.i(any()) } just Runs
        every { appRouter.popTo(any<AppRoute>(), any()) } answers { secondArg<(Boolean) -> Unit>().invoke(true) }
        coEvery { userWalletsListRepository.selectedUserWalletSync() } returns unlockedWallet()
        every { paymentAccountStatusSupplier.invoke(any<UserWalletId>()) } returns flowOf(paymentStatus())
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(TangemLogger)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ScreenResolution {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN screen query param WHEN deeplink handled THEN details open on the requested screen`(
            model: ScreenModel,
        ) = runTest {
            // Arrange
            val status = paymentStatus()
            every { paymentAccountStatusSupplier.invoke(any<UserWalletId>()) } returns flowOf(status)

            // Act
            createHandler(scope = this, queryParams = model.queryParams)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) {
                appRouter.push(
                    route = AppRoute.TangemPayDetails(status = status, initialRoute = model.expectedRoute),
                    onComplete = any(),
                )
            }
        }

        private fun provideTestModels() = listOf(
            ScreenModel(queryParams = emptyMap(), expectedRoute = TangemPayDetailsInitialRoute.ACCOUNT_DETAILS),
            ScreenModel(screen = "add_funds", expectedRoute = TangemPayDetailsInitialRoute.ADD_FUNDS),
            ScreenModel(screen = "va_onramp", expectedRoute = TangemPayDetailsInitialRoute.VA_ONRAMP),
            ScreenModel(screen = "va_onramp_details", expectedRoute = TangemPayDetailsInitialRoute.VA_ONRAMP_DETAILS),
            ScreenModel(screen = "select_plan", expectedRoute = TangemPayDetailsInitialRoute.TIERS_ONBOARDING),
            ScreenModel(screen = "current_plan", expectedRoute = TangemPayDetailsInitialRoute.CURRENT_PLAN),
            ScreenModel(screen = "change_plan", expectedRoute = TangemPayDetailsInitialRoute.CHANGE_PLAN),
            ScreenModel(screen = "cashback", expectedRoute = TangemPayDetailsInitialRoute.CASHBACK),
            ScreenModel(screen = "order_card", expectedRoute = TangemPayDetailsInitialRoute.ORDER_CARD),
            ScreenModel(screen = "CASHBACK", expectedRoute = TangemPayDetailsInitialRoute.CASHBACK),
            ScreenModel(screen = "unknown", expectedRoute = TangemPayDetailsInitialRoute.ACCOUNT_DETAILS),
            ScreenModel(screen = " ", expectedRoute = TangemPayDetailsInitialRoute.ACCOUNT_DETAILS),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AccountGating {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN payment account status WHEN deeplink handled THEN details open only for openable statuses`(
            model: StatusModel,
        ) = runTest {
            // Arrange
            every { paymentAccountStatusSupplier.invoke(any<UserWalletId>()) } returns flowOf(paymentStatus(model.value))

            // Act
            createHandler(scope = this)
            advanceUntilIdle()

            // Assert
            verify(exactly = if (model.isOpenable) 1 else 0) {
                appRouter.push(any<AppRoute.TangemPayDetails>(), any())
            }
            verify(atLeast = 1) { appRouter.popTo(route = AppRoute.Wallet, onComplete = any()) }
        }

        @Test
        fun `GIVEN no selected wallet WHEN deeplink handled THEN wallet screen opens and details do not`() = runTest {
            // Arrange
            coEvery { userWalletsListRepository.selectedUserWalletSync() } returns null

            // Act
            createHandler(scope = this)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { appRouter.popTo(route = AppRoute.Wallet, onComplete = any()) }
            verify(exactly = 0) { appRouter.push(any<AppRoute.TangemPayDetails>(), any()) }
        }

        @Test
        fun `GIVEN locked selected wallet WHEN deeplink handled THEN details do not open`() = runTest {
            // Arrange
            coEvery { userWalletsListRepository.selectedUserWalletSync() } returns lockedWallet()

            // Act
            createHandler(scope = this)
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { appRouter.push(any<AppRoute.TangemPayDetails>(), any()) }
        }

        @Test
        fun `GIVEN status is never emitted WHEN deeplink handled THEN details do not open`() = runTest {
            // Arrange
            every { paymentAccountStatusSupplier.invoke(any<UserWalletId>()) } returns emptyFlow()

            // Act
            createHandler(scope = this)
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { appRouter.push(any<AppRoute.TangemPayDetails>(), any()) }
        }

        private fun provideTestModels() = listOf(
            StatusModel(PaymentAccountStatusValue.Empty, isOpenable = false),
            StatusModel(PaymentAccountStatusValue.NotCreated, isOpenable = false),
            StatusModel(PaymentAccountStatusValue.Error.Unavailable, isOpenable = false),
            StatusModel(PaymentAccountStatusValue.Error.NotSynced, isOpenable = false),
            StatusModel(PaymentAccountStatusValue.Error.ExposedDevice, isOpenable = false),
            StatusModel(PaymentAccountStatusValue.Error.CardIssueFailed("customer-id"), isOpenable = false),
            StatusModel(mockk<PaymentAccountStatusValue.UnderReview>(relaxed = true), isOpenable = false),
            StatusModel(mockk<PaymentAccountStatusValue.IssuingCard>(relaxed = true), isOpenable = false),
            StatusModel(PaymentAccountStatusValue.Loading, isOpenable = true),
            StatusModel(mockk<PaymentAccountStatusValue.AwaitingPlanSelection>(relaxed = true), isOpenable = true),
            StatusModel(mockk<PaymentAccountStatusValue.Inactive>(relaxed = true), isOpenable = true),
            StatusModel(mockk<PaymentAccountStatusValue.Loaded>(relaxed = true), isOpenable = true),
            StatusModel(mockk<PaymentAccountStatusValue.Deactivated>(relaxed = true), isOpenable = true),
        )
    }

    private fun createHandler(scope: TestScope, queryParams: Map<String, String> = emptyMap()) =
        DefaultTangemPayAccountDeepLinkHandler(
            scope = scope,
            queryParams = queryParams,
            appRouter = appRouter,
            userWalletsListRepository = userWalletsListRepository,
            paymentAccountStatusFetcher = paymentAccountStatusFetcher,
            paymentAccountStatusSupplier = paymentAccountStatusSupplier,
        )

    private fun unlockedWallet(id: UserWalletId = USER_WALLET_ID): UserWallet = mockk<UserWallet.Hot>(relaxed = true) {
        every { walletId } returns id
        every { isLocked } returns false
    }

    private fun lockedWallet(id: UserWalletId = USER_WALLET_ID): UserWallet = mockk<UserWallet.Hot>(relaxed = true) {
        every { walletId } returns id
        every { isLocked } returns true
    }

    private fun paymentStatus(value: PaymentAccountStatusValue = loadedValue()): AccountStatus.Payment =
        mockk(relaxed = true) {
            every { this@mockk.value } returns value
        }

    private fun loadedValue(statusSource: StatusSource = StatusSource.ACTUAL): PaymentAccountStatusValue.Loaded =
        mockk(relaxed = true) {
            every { source } returns statusSource
        }

    internal data class ScreenModel(
        val queryParams: Map<String, String>,
        val expectedRoute: TangemPayDetailsInitialRoute,
    ) {
        constructor(screen: String, expectedRoute: TangemPayDetailsInitialRoute) : this(
            queryParams = mapOf("screen" to screen),
            expectedRoute = expectedRoute,
        )

        override fun toString(): String = "${queryParams["screen"] ?: "no screen"} -> $expectedRoute"
    }

    internal data class StatusModel(val value: PaymentAccountStatusValue, val isOpenable: Boolean) {
        override fun toString(): String = "${value::class.simpleName} -> $isOpenable"
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
    }
}