package com.tangem.features.tangempay.account.cashback

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CashbackDisplayMode
import com.tangem.domain.pay.model.CashbackSummary
import com.tangem.domain.pay.model.TangemPayCashback
import com.tangem.domain.pay.usecase.GetCashbackDeactivationDismissedUseCase
import com.tangem.domain.pay.usecase.GetCashbackSummaryUseCase
import com.tangem.domain.pay.usecase.ObserveCashbackSummaryUseCase
import com.tangem.domain.pay.usecase.SetCashbackDeactivationDismissedUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.tangempay.account.CashbackBlockUM
import com.tangem.features.tangempay.account.TangemPayAccountDetailsInnerRoute
import com.tangem.test.core.ProvideTestModels
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal
import kotlin.reflect.KClass

private const val REQUEST_DELAY_MILLIS = 100L

@OptIn(ExperimentalCoroutinesApi::class)
internal class TangemPayCashbackControllerTest {

    private val userWalletId = UserWalletId("123")

    private val observeCashbackSummaryUseCase: ObserveCashbackSummaryUseCase = mockk()
    private val getCashbackSummaryUseCase: GetCashbackSummaryUseCase = mockk()
    private val getCashbackDeactivationDismissedUseCase: GetCashbackDeactivationDismissedUseCase = mockk()
    private val setCashbackDeactivationDismissedUseCase: SetCashbackDeactivationDismissedUseCase = mockk()
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)
    private val router: Router = mockk(relaxed = true)

    private val cachedSummary = MutableStateFlow<CashbackSummary?>(null)
    private val accountStatus = MutableStateFlow(paymentStatus(loadedStatus()))

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            observeCashbackSummaryUseCase,
            getCashbackSummaryUseCase,
            getCashbackDeactivationDismissedUseCase,
            setCashbackDeactivationDismissedUseCase,
            analytics,
            router,
        )
        cachedSummary.value = null
        accountStatus.value = paymentStatus(loadedStatus())
        every { observeCashbackSummaryUseCase(any()) } returns cachedSummary
        coEvery { getCashbackDeactivationDismissedUseCase(any()) } returns false
        coEvery { setCashbackDeactivationDismissedUseCase(any()) } just Runs
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class State {

        @Test
        fun `GIVEN cached full summary WHEN created THEN widget shown without a refresh`() = runTest {
            // Arrange
            cachedSummary.value = enabledSummary(CashbackDisplayMode.FULL)

            // Act
            val controller = createController(scope = backgroundScope)
            runCurrent()

            // Assert
            assertThat(controller.state.value.block).isInstanceOf(CashbackBlockUM.Widget::class.java)
            assertThat(controller.state.value.menuItem).isNull()
            coVerify(exactly = 0) { getCashbackSummaryUseCase(any()) }
        }

        @Test
        fun `GIVEN cached alt_block summary WHEN created THEN menu item shown instead of the block`() = runTest {
            // Arrange
            cachedSummary.value = enabledSummary(CashbackDisplayMode.ALT_BLOCK)

            // Act
            val controller = createController(scope = backgroundScope)
            runCurrent()

            // Assert
            assertThat(controller.state.value.block).isNull()
            assertThat(controller.state.value.menuItem).isNotNull()
        }

        @Test
        fun `GIVEN cached widget WHEN status re-emits THEN widget survives and no refresh is sent`() = runTest {
            // Arrange
            cachedSummary.value = enabledSummary(CashbackDisplayMode.FULL)
            val controller = createController(scope = backgroundScope)
            runCurrent()

            // Act
            accountStatus.value = paymentStatus(loadedStatus())
            runCurrent()

            // Assert
            assertThat(controller.state.value.block).isInstanceOf(CashbackBlockUM.Widget::class.java)
            coVerify(exactly = 0) { getCashbackSummaryUseCase(any()) }
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN widget shown WHEN account leaves loaded THEN state is empty`(model: NotLoadedStatusModel) = runTest {
            // Arrange
            cachedSummary.value = enabledSummary(CashbackDisplayMode.FULL)
            val controller = createController(scope = backgroundScope)
            runCurrent()

            // Act
            accountStatus.value = paymentStatus(model.status)
            runCurrent()

            // Assert
            assertThat(controller.state.value).isEqualTo(CashbackUM())
        }

        @Test
        fun `GIVEN nothing cached WHEN refresh succeeds THEN widget shown`() = runTest {
            // Arrange
            givenRefreshReturns(enabledSummary(CashbackDisplayMode.FULL).right())
            val controller = createController(scope = backgroundScope)
            runCurrent()

            // Act
            controller.refresh()
            runCurrent()

            // Assert
            assertThat(controller.state.value.block).isInstanceOf(CashbackBlockUM.Widget::class.java)
            coVerify(exactly = 1) { getCashbackSummaryUseCase(userWalletId) }
        }

        @ParameterizedTest
        @EnumSource(CashbackDisplayMode::class)
        fun `GIVEN cached summary WHEN refresh fails THEN cached cashback stays and no error shown`(
            displayMode: CashbackDisplayMode,
        ) = runTest {
            // Arrange
            cachedSummary.value = enabledSummary(displayMode)
            givenRefreshReturns(refreshFailure())
            val controller = createController(scope = backgroundScope)
            runCurrent()
            val stateBeforeRefresh = controller.state.value

            // Act
            controller.refresh()
            runCurrent()

            // Assert
            assertThat(controller.state.value).isEqualTo(stateBeforeRefresh)
            verify(exactly = 0) { analytics.send(ofType<TangemPayAnalyticsEvents.Cashback.BannerErrorStateShowed>()) }
        }

        @Test
        fun `GIVEN cached widget WHEN refresh returns disabled THEN cashback hidden`() = runTest {
            // Arrange
            cachedSummary.value = enabledSummary(CashbackDisplayMode.FULL)
            givenRefreshReturns(CashbackSummary.Disabled.right())
            val controller = createController(scope = backgroundScope)
            runCurrent()

            // Act
            controller.refresh()
            runCurrent()

            // Assert
            assertThat(controller.state.value).isEqualTo(CashbackUM())
        }

        @Test
        fun `GIVEN nothing cached WHEN refresh fails THEN error block shown`() = runTest {
            // Arrange
            givenRefreshReturns(refreshFailure())
            val controller = createController(scope = backgroundScope)
            runCurrent()

            // Act
            controller.refresh()
            runCurrent()

            // Assert
            assertThat(controller.errorBlock().isReloading).isFalse()
            assertThat(controller.state.value.menuItem).isNull()
        }

        @Test
        fun `GIVEN error block WHEN reload tapped and refresh succeeds THEN widget shown`() = runTest {
            // Arrange
            givenRefreshReturns(refreshFailure())
            val controller = createController(scope = backgroundScope)
            controller.refresh()
            runCurrent()
            val errorBlock = controller.errorBlock()
            givenRefreshReturns(enabledSummary(CashbackDisplayMode.FULL).right())

            // Act
            errorBlock.onReload()
            runCurrent()

            // Assert
            assertThat(controller.state.value.block).isInstanceOf(CashbackBlockUM.Widget::class.java)
            coVerify(exactly = 2) { getCashbackSummaryUseCase(userWalletId) }
        }

        @Test
        fun `GIVEN error block WHEN reload tapped THEN progress shown while request in flight`() = runTest {
            // Arrange
            givenRefreshReturns(refreshFailure())
            val controller = createController(scope = backgroundScope)
            controller.refresh()
            runCurrent()
            val errorBlock = controller.errorBlock()
            givenRefreshReturns(refreshFailure(), delayMillis = REQUEST_DELAY_MILLIS)

            // Act
            errorBlock.onReload()
            runCurrent()

            // Assert
            assertThat(controller.errorBlock().isReloading).isTrue()
            advanceTimeBy(REQUEST_DELAY_MILLIS)
            runCurrent()
            assertThat(controller.errorBlock().isReloading).isFalse()
        }

        private fun provideTestModels(): List<NotLoadedStatusModel> = listOf(
            NotLoadedStatusModel(name = "inactive", status = inactiveStatus()),
            NotLoadedStatusModel(name = "deactivated", status = deactivatedStatus()),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Refresh {

        @Test
        fun `GIVEN refresh in flight WHEN refresh called again THEN one request sent`() = runTest {
            // Arrange
            givenRefreshReturns(enabledSummary(CashbackDisplayMode.FULL).right(), delayMillis = REQUEST_DELAY_MILLIS)
            val controller = createController(scope = backgroundScope)
            controller.refresh()
            runCurrent()

            // Act
            controller.refresh()
            advanceTimeBy(REQUEST_DELAY_MILLIS)
            runCurrent()

            // Assert
            coVerify(exactly = 1) { getCashbackSummaryUseCase(userWalletId) }
        }

        @Test
        fun `GIVEN deactivated account WHEN refresh THEN summary not requested`() = runTest {
            // Arrange
            accountStatus.value = paymentStatus(deactivatedStatus())
            val controller = createController(scope = backgroundScope)
            runCurrent()

            // Act
            controller.refresh()
            runCurrent()

            // Assert
            coVerify(exactly = 0) { getCashbackSummaryUseCase(any()) }
        }

        @Test
        fun `GIVEN refresh in flight WHEN account becomes deactivated THEN request cancelled and progress reset`() =
            runTest {
                // Arrange
                givenRefreshReturns(refreshFailure())
                val controller = createController(scope = backgroundScope)
                controller.refresh()
                runCurrent()
                givenRefreshReturns(
                    result = enabledSummary(CashbackDisplayMode.FULL).right(),
                    delayMillis = REQUEST_DELAY_MILLIS,
                )
                controller.refresh()
                runCurrent()

                // Act
                accountStatus.value = paymentStatus(deactivatedStatus())
                runCurrent()
                advanceTimeBy(REQUEST_DELAY_MILLIS)
                runCurrent()
                val stateWhileDeactivated = controller.state.value
                accountStatus.value = paymentStatus(loadedStatus())
                runCurrent()

                // Assert
                assertThat(stateWhileDeactivated).isEqualTo(CashbackUM())
                assertThat(cachedSummary.value).isNull()
                assertThat(controller.errorBlock().isReloading).isFalse()
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class DeactivationBanner {

        @Test
        fun `GIVEN deactivation already dismissed WHEN created THEN block hidden and no impression sent`() = runTest {
            // Arrange
            cachedSummary.value = CashbackSummary.Deactivated
            coEvery { getCashbackDeactivationDismissedUseCase(userWalletId) } returns true

            // Act
            val controller = createController(scope = backgroundScope)
            runCurrent()

            // Assert
            assertThat(controller.state.value).isEqualTo(CashbackUM())
            verify(exactly = 0) { analytics.send(any()) }
        }

        @Test
        fun `GIVEN deactivation banner WHEN got it tapped THEN banner hidden before the dismissal is saved`() =
            runTest {
                // Arrange
                cachedSummary.value = CashbackSummary.Deactivated
                coEvery { setCashbackDeactivationDismissedUseCase(any()) } coAnswers { delay(REQUEST_DELAY_MILLIS) }
                val controller = createController(scope = backgroundScope)
                runCurrent()
                val banner = controller.state.value.block as CashbackBlockUM.DeactivatedBanner

                // Act
                banner.onGotIt()
                runCurrent()

                // Assert
                assertThat(controller.state.value).isEqualTo(CashbackUM())
                verify(exactly = 1) {
                    analytics.send(ofType<TangemPayAnalyticsEvents.Cashback.DeactivationBannerGotItClicked>())
                }
                coVerify(exactly = 1) { setCashbackDeactivationDismissedUseCase(userWalletId) }
            }

        @Test
        fun `GIVEN dismissed banner WHEN refresh returns deactivated again THEN banner stays hidden`() = runTest {
            // Arrange
            cachedSummary.value = CashbackSummary.Deactivated
            givenRefreshReturns(CashbackSummary.Deactivated.right())
            val controller = createController(scope = backgroundScope)
            runCurrent()
            (controller.state.value.block as CashbackBlockUM.DeactivatedBanner).onGotIt()
            runCurrent()

            // Act
            controller.refresh()
            runCurrent()

            // Assert
            assertThat(controller.state.value).isEqualTo(CashbackUM())
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Impressions {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN cached summary on loaded account WHEN created THEN only the matching impression sent once`(
            model: ImpressionModel,
        ) = runTest {
            // Arrange
            cachedSummary.value = model.summary

            // Act
            createController(scope = backgroundScope)
            runCurrent()

            // Assert
            verify(exactly = 1) { analytics.send(match { model.expectedEvent.isInstance(it) }) }
            verify(exactly = 1) { analytics.send(any()) }
        }

        @Test
        fun `GIVEN failed refresh without cache WHEN status re-emits THEN error impression sent once`() = runTest {
            // Arrange
            givenRefreshReturns(refreshFailure())
            val controller = createController(scope = backgroundScope)
            controller.refresh()
            runCurrent()

            // Act
            accountStatus.value = paymentStatus(loadedStatus())
            runCurrent()

            // Assert
            verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.Cashback.BannerErrorStateShowed>()) }
        }

        @Test
        fun `GIVEN widget shown WHEN status re-emits and refresh succeeds THEN banner impression sent once`() =
            runTest {
                // Arrange
                cachedSummary.value = enabledSummary(CashbackDisplayMode.FULL)
                givenRefreshReturns(enabledSummary(CashbackDisplayMode.FULL).right())
                val controller = createController(scope = backgroundScope)
                runCurrent()

                // Act
                accountStatus.value = paymentStatus(loadedStatus())
                controller.refresh()
                runCurrent()

                // Assert
                verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.Cashback.BannerShowed>()) }
            }

        @Test
        fun `GIVEN widget impression sent WHEN cashback hidden and shown again THEN impression sent again`() =
            runTest {
                // Arrange
                cachedSummary.value = enabledSummary(CashbackDisplayMode.FULL)
                createController(scope = backgroundScope)
                runCurrent()

                // Act
                cachedSummary.value = null
                runCurrent()
                cachedSummary.value = enabledSummary(CashbackDisplayMode.FULL)
                runCurrent()

                // Assert
                verify(exactly = 2) { analytics.send(ofType<TangemPayAnalyticsEvents.Cashback.BannerShowed>()) }
            }

        @Test
        fun `GIVEN error impression sent WHEN reload fails again THEN impression not repeated`() = runTest {
            // Arrange
            givenRefreshReturns(refreshFailure())
            val controller = createController(scope = backgroundScope)
            controller.refresh()
            runCurrent()
            givenRefreshReturns(refreshFailure(), delayMillis = REQUEST_DELAY_MILLIS)

            // Act
            controller.errorBlock().onReload()
            runCurrent()
            advanceTimeBy(REQUEST_DELAY_MILLIS)
            runCurrent()

            // Assert
            verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.Cashback.BannerErrorStateShowed>()) }
        }

        @Test
        fun `GIVEN widget impression sent WHEN cashback amount changes THEN impression not repeated`() = runTest {
            // Arrange
            cachedSummary.value = enabledSummary(CashbackDisplayMode.FULL)
            createController(scope = backgroundScope)
            runCurrent()

            // Act
            cachedSummary.value = enabledSummary(CashbackDisplayMode.FULL, confirmedAmount = BigDecimal("5.40"))
            runCurrent()

            // Assert
            verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.Cashback.BannerShowed>()) }
        }

        @Test
        fun `GIVEN inactive account WHEN summary cached THEN no impression sent`() = runTest {
            // Arrange
            accountStatus.value = paymentStatus(inactiveStatus())
            createController(scope = backgroundScope)
            runCurrent()

            // Act
            cachedSummary.value = enabledSummary(CashbackDisplayMode.FULL)
            runCurrent()

            // Assert
            verify(exactly = 0) { analytics.send(any()) }
        }

        @Test
        fun `GIVEN widget impression sent WHEN account leaves loaded and returns THEN impression not repeated`() =
            runTest {
                // Arrange
                cachedSummary.value = enabledSummary(CashbackDisplayMode.FULL)
                createController(scope = backgroundScope)
                runCurrent()

                // Act
                accountStatus.value = paymentStatus(inactiveStatus())
                runCurrent()
                accountStatus.value = paymentStatus(loadedStatus())
                runCurrent()

                // Assert
                verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.Cashback.BannerShowed>()) }
            }

        @Test
        fun `GIVEN widget cached on inactive account WHEN account becomes loaded THEN banner impression sent`() =
            runTest {
                // Arrange
                cachedSummary.value = enabledSummary(CashbackDisplayMode.FULL)
                accountStatus.value = paymentStatus(inactiveStatus())
                createController(scope = backgroundScope)
                runCurrent()

                // Act
                accountStatus.value = paymentStatus(loadedStatus())
                runCurrent()

                // Assert
                verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.Cashback.BannerShowed>()) }
            }

        private fun provideTestModels(): List<ImpressionModel> = listOf(
            ImpressionModel(
                summary = enabledSummary(CashbackDisplayMode.FULL),
                expectedEvent = TangemPayAnalyticsEvents.Cashback.BannerShowed::class,
            ),
            ImpressionModel(
                summary = enabledSummary(CashbackDisplayMode.ALT_BLOCK),
                expectedEvent = TangemPayAnalyticsEvents.Cashback.ButtonInSettingsShowed::class,
            ),
            ImpressionModel(
                summary = CashbackSummary.Deactivated,
                expectedEvent = TangemPayAnalyticsEvents.Cashback.DeactivationBannerShowed::class,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Clicks {

        @Test
        fun `GIVEN widget WHEN clicked THEN banner clicked sent and cashback screen opened`() = runTest {
            // Arrange
            cachedSummary.value = enabledSummary(CashbackDisplayMode.FULL)
            val controller = createController(scope = backgroundScope)
            runCurrent()
            val widget = controller.state.value.block as CashbackBlockUM.Widget

            // Act
            widget.onClick()

            // Assert
            verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.Cashback.BannerClicked>()) }
            verify(exactly = 0) { analytics.send(ofType<TangemPayAnalyticsEvents.Cashback.ButtonInSettingsClicked>()) }
            verify(exactly = 1) { router.push(TangemPayAccountDetailsInnerRoute.Cashback, any()) }
        }

        @Test
        fun `GIVEN menu item WHEN clicked THEN button in settings clicked sent and cashback screen opened`() =
            runTest {
                // Arrange
                cachedSummary.value = enabledSummary(CashbackDisplayMode.ALT_BLOCK)
                val controller = createController(scope = backgroundScope)
                runCurrent()
                val menuItem = requireNotNull(controller.state.value.menuItem)

                // Act
                menuItem.onClick()

                // Assert
                verify(exactly = 1) {
                    analytics.send(ofType<TangemPayAnalyticsEvents.Cashback.ButtonInSettingsClicked>())
                }
                verify(exactly = 0) { analytics.send(ofType<TangemPayAnalyticsEvents.Cashback.BannerClicked>()) }
                verify(exactly = 1) { router.push(TangemPayAccountDetailsInnerRoute.Cashback, any()) }
            }
    }

    private fun createController(scope: CoroutineScope): TangemPayCashbackController = TangemPayCashbackController(
        scope = scope,
        accountStatus = accountStatus,
        observeCashbackSummaryUseCase = observeCashbackSummaryUseCase,
        getCashbackSummaryUseCase = getCashbackSummaryUseCase,
        getCashbackDeactivationDismissedUseCase = getCashbackDeactivationDismissedUseCase,
        setCashbackDeactivationDismissedUseCase = setCashbackDeactivationDismissedUseCase,
        analytics = analytics,
        router = router,
    )

    private fun givenRefreshReturns(result: Either<VisaApiError, CashbackSummary>, delayMillis: Long = 0L) {
        coEvery { getCashbackSummaryUseCase(any()) } coAnswers {
            delay(delayMillis)
            result.onRight { summary ->
                cachedSummary.value = summary.takeIf {
                    it is CashbackSummary.Enabled || it is CashbackSummary.Deactivated
                }
            }
        }
    }

    private fun refreshFailure(): Either<VisaApiError, CashbackSummary> = mockk<VisaApiError>(relaxed = true).left()

    private fun TangemPayCashbackController.errorBlock(): CashbackBlockUM.Error =
        state.value.block as CashbackBlockUM.Error

    private fun enabledSummary(
        displayMode: CashbackDisplayMode,
        confirmedAmount: BigDecimal = BigDecimal("2.70"),
    ): CashbackSummary.Enabled = CashbackSummary.Enabled(
        displayMode = displayMode,
        cashback = TangemPayCashback(
            confirmedAmount = confirmedAmount,
            totalEarnedAmount = BigDecimal("2.70"),
            currency = "USD",
            payoutCurrency = "USDC",
            period = TangemPayCashback.Period(year = 2026, month = 8, payoutStart = null, payoutEnd = null),
            previousPayout = null,
        ),
    )

    private fun paymentStatus(value: PaymentAccountStatusValue): AccountStatus.Payment =
        AccountStatus.Payment(account = Account.Payment(userWalletId), value = value)

    private fun loadedStatus(): PaymentAccountStatusValue.Loaded = mockk()

    private fun inactiveStatus(): PaymentAccountStatusValue.Inactive = mockk()

    private fun deactivatedStatus(): PaymentAccountStatusValue.Deactivated = mockk()

    internal data class ImpressionModel(
        val summary: CashbackSummary,
        val expectedEvent: KClass<out TangemPayAnalyticsEvents>,
    )

    internal data class NotLoadedStatusModel(val name: String, val status: PaymentAccountStatusValue) {
        override fun toString(): String = name
    }
}