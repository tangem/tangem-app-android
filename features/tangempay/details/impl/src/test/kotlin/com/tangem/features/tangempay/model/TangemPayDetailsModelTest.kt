package com.tangem.features.tangempay.model

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.domain.models.account.VirtualAccountOnramp
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.pay.TangemPayDetailsInitialRoute
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.features.tangempay.addFundsButton
import com.tangem.features.tangempay.components.TangemPayDetailsContainerComponent
import com.tangem.features.tangempay.entity.TangemPayDetailsBalanceBlockState
import com.tangem.features.tangempay.navigation.TangemPayAccountDetailsInnerRoute
import com.tangem.features.tangempay.tiers.select.TangemPaySelectPlanSource
import com.tangem.features.tangempay.tangemPayCard
import com.tangem.features.tangempay.withdrawButton
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class TangemPayDetailsModelTest {

    private val userWalletId = UserWalletId("123")

    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier = mockk()
    private val cardDetailsRepository: TangemPayCardDetailsRepository = mockk(relaxed = true)
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)
    private val router: Router = mockk(relaxed = true)

    @ParameterizedTest
    @MethodSource("provideMutedCases")
    fun `GIVEN status source WHEN status loaded THEN balance is muted only when cached`(case: MutedCase) = runTest {
        // Arrange + Act
        val model = createModel(
            testScope = this,
            statusSource = case.statusSource,
            frozenState = TangemPayCardFrozenState.Unfrozen,
            availableForWithdrawal = BigDecimal.ZERO,
            accountError = case.accountError,
        )
        advanceUntilIdle()

        // Assert
        val balanceState = model.uiState.value.balanceBlockState
        assertThat(balanceState).isInstanceOf(TangemPayDetailsBalanceBlockState.Content::class.java)
        assertThat((balanceState as TangemPayDetailsBalanceBlockState.Content).isMuted).isEqualTo(case.expectedMuted)
        model.onDestroy()
    }

    @ParameterizedTest
    @MethodSource("provideTransactionClickCases")
    fun `GIVEN status WHEN transaction clicked THEN opens details only when customerId present`(
        case: TransactionClickCase,
    ) = runTest {
        // Arrange
        val model = createModel(testScope = this, statusValue = case.status)
        advanceUntilIdle()

        // Act
        model.onTransactionClick(mockk<TangemPayTxHistoryItem.Payment>(relaxed = true))

        // Assert
        verify(exactly = if (case.expectedOpened) 1 else 0) {
            analytics.send(ofType<TangemPayAnalyticsEvents.TransactionInListClicked>())
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN virtual account is processing WHEN bank transfer clicked THEN preparation popup event sent`() =
        runTest {
            // GIVEN
            val model = createModel(testScope = this, virtualAccount = VirtualAccountOnramp.Processing)
            advanceUntilIdle()

            // WHEN
            model.onClickBankTransfer()

            // THEN
            verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.VaPreparationPopupShowed>()) }
            verify(exactly = 0) { analytics.send(ofType<TangemPayAnalyticsEvents.VaTopupButtonClicked>()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN loaded status WHEN banking details error shown THEN details error event sent`() = runTest {
        // GIVEN
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // WHEN
        model.showVaBankingDetailsError()

        // THEN
        verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.VaDetailsErrorShowed>()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN awaiting plan selection WHEN model created THEN inner stack is replaced with plan selection`() =
        runTest {
            // GIVEN
            val tariffPlan: TangemPayCustomerTariffPlan = mockk(relaxed = true)
            val awaitingPlanSelection = PaymentAccountStatusValue.AwaitingPlanSelection(
                source = StatusSource.ACTUAL,
                tariffPlan = tariffPlan,
            )

            // WHEN
            val model = createModel(testScope = this, statusValue = awaitingPlanSelection)
            advanceUntilIdle()

            // THEN
            verify(exactly = 1) {
                router.replaceAll(
                    TangemPayAccountDetailsInnerRoute.SelectPlan(
                        tariffPlan = tariffPlan,
                        source = TangemPaySelectPlanSource.TIERS_ONBOARDING,
                    ),
                )
            }
            verify(exactly = 0) { router.push(any(), any()) }
            model.onDestroy()
        }

    private fun createModel(
        testScope: TestScope,
        statusSource: StatusSource = StatusSource.ACTUAL,
        frozenState: TangemPayCardFrozenState = TangemPayCardFrozenState.Unfrozen,
        availableForWithdrawal: BigDecimal = BigDecimal.ZERO,
        accountError: PaymentAccountStatusValue.Error? = null,
        statusValue: PaymentAccountStatusValue? = null,
        virtualAccount: VirtualAccountOnramp? = null,
    ): TangemPayDetailsModel {
        val loaded: PaymentAccountStatusValue.Loaded = mockk(relaxed = true) {
            every { source } returns statusSource
            every { error } returns accountError
            every { customerId } returns "customer-id"
            every { this@mockk.virtualAccount } returns virtualAccount
            every { cards } returns listOf(tangemPayCard())
            every { balance } returns PaymentAccountStatusValue.Balance(
                fiatBalance = PaymentAccountStatusValue.FiatBalance(
                    availableBalance = BigDecimal.ZERO,
                    currency = "USD",
                ),
                cryptoBalance = PaymentAccountStatusValue.CryptoBalance(
                    id = "id",
                    chainId = 1L,
                    depositAddress = "address",
                    tokenContractAddress = "contract",
                    balance = BigDecimal.ZERO,
                ),
                availableForWithdrawal = availableForWithdrawal,
            )
        }
        val paymentStatus: AccountStatus.Payment = mockk(relaxed = true) {
            every { value } returns (statusValue ?: loaded)
            every { account } returns mockk(relaxed = true) {
                every { userWalletId } returns this@TangemPayDetailsModelTest.userWalletId
            }
        }
        val params = TangemPayDetailsContainerComponent.Params(
            initialStatus = paymentStatus,
            initialRoute = TangemPayDetailsInitialRoute.ACCOUNT_DETAILS,
        )

        every { paymentAccountStatusSupplier.invoke(any<UserWalletId>()) } returns flowOf(paymentStatus)
        every { cardDetailsRepository.cardFrozenState(any()) } returns flowOf(frozenState)
        coEvery { cardDetailsRepository.isAddToWalletDone(any()) } returns false.right()

        return TangemPayDetailsModel(
            paramsContainer = MutableParamsContainer(params),
            paymentAccountStatusSupplier = paymentAccountStatusSupplier,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            analytics = analytics,
            router = router,
            urlOpener = mockk(relaxed = true),
            cardDetailsRepository = cardDetailsRepository,
            getBalanceHidingSettingsUseCase = mockk(relaxed = true),
            uiMessageSender = mockk(relaxed = true),
            txHistoryUpdateListener = mockk(relaxed = true),
            tangemPayWithdrawRepository = mockk(relaxed = true),
            sendFeedbackEmailUseCase = mockk(relaxed = true),
            expressTransactionsEventListener = mockk(relaxed = true),
            tangemPayFeatureToggles = mockk(relaxed = true),
            paymentAccountStatusFetcher = mockk(relaxed = true),
            produceTangemPayInitialDataUseCase = mockk(relaxed = true),
            onboardingRepository = mockk(relaxed = true),
            getCustomerOffers = mockk(relaxed = true),
            cancelTariffTransitionUseCase = mockk(relaxed = true),
            getCashbackSummaryUseCase = mockk(relaxed = true),
            getCashbackDeactivationDismissedUseCase = mockk(relaxed = true),
            setCashbackDeactivationDismissedUseCase = mockk(relaxed = true),
            tangemPayCurrencyFactory = mockk(relaxed = true),
        )
    }

    private fun TestScope.createTestingCoroutineDispatcherProvider(): TestingCoroutineDispatcherProvider {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        return TestingCoroutineDispatcherProvider(
            main = testDispatcher,
            mainImmediate = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
            single = testDispatcher,
        )
    }

    internal data class FreezeCase(
        val statusSource: StatusSource,
        val frozenState: TangemPayCardFrozenState,
        val availableForWithdrawal: BigDecimal,
        val expectedAddFundsEnabled: Boolean,
        val expectedWithdrawEnabled: Boolean,
    )

    internal data class MutedCase(
        val statusSource: StatusSource,
        val expectedMuted: Boolean,
        val accountError: PaymentAccountStatusValue.Error? = null,
    )

    internal data class TransactionClickCase(
        val name: String,
        val status: PaymentAccountStatusValue?,
        val expectedOpened: Boolean,
    ) {
        override fun toString(): String = name
    }

    private companion object {
        @JvmStatic
        fun provideFreezeCases() = listOf(
            FreezeCase(
                statusSource = StatusSource.ACTUAL,
                frozenState = TangemPayCardFrozenState.Unfrozen,
                availableForWithdrawal = BigDecimal.ZERO,
                expectedAddFundsEnabled = true,
                expectedWithdrawEnabled = false,
            ),
            FreezeCase(
                statusSource = StatusSource.ACTUAL,
                frozenState = TangemPayCardFrozenState.Unfrozen,
                availableForWithdrawal = BigDecimal.TEN,
                expectedAddFundsEnabled = true,
                expectedWithdrawEnabled = true,
            ),
            FreezeCase(
                statusSource = StatusSource.ACTUAL,
                frozenState = TangemPayCardFrozenState.Frozen,
                availableForWithdrawal = BigDecimal.TEN,
                expectedAddFundsEnabled = false,
                expectedWithdrawEnabled = false,
            ),
            FreezeCase(
                statusSource = StatusSource.CACHE,
                frozenState = TangemPayCardFrozenState.Unfrozen,
                availableForWithdrawal = BigDecimal.TEN,
                expectedAddFundsEnabled = false,
                expectedWithdrawEnabled = false,
            ),
        )

        @JvmStatic
        fun provideMutedCases() = listOf(
            MutedCase(statusSource = StatusSource.ACTUAL, expectedMuted = false),
            MutedCase(statusSource = StatusSource.CACHE, expectedMuted = true),
            MutedCase(statusSource = StatusSource.ONLY_CACHE, expectedMuted = true),
            // ACTUAL but errored is still not fresh -> muted (guards !isFresh, not just source != ACTUAL)
            MutedCase(
                statusSource = StatusSource.ACTUAL,
                accountError = PaymentAccountStatusValue.Error.Unavailable,
                expectedMuted = true,
            ),
        )

        @JvmStatic
        fun provideTransactionClickCases() = listOf(
            TransactionClickCase(
                name = "deactivated account (has customerId) -> opens",
                status = deactivatedStatus(id = "customer-id"),
                expectedOpened = true,
            ),
            TransactionClickCase(
                name = "loaded account (has customerId) -> opens",
                status = null,
                expectedOpened = true,
            ),
            TransactionClickCase(
                name = "loading status (no customerId) -> ignored",
                status = PaymentAccountStatusValue.Loading,
                expectedOpened = false,
            ),
            TransactionClickCase(
                name = "not created status (no customerId) -> ignored",
                status = PaymentAccountStatusValue.NotCreated,
                expectedOpened = false,
            ),
        )

        private fun deactivatedStatus(id: String): PaymentAccountStatusValue.Deactivated = mockk(relaxed = true) {
            every { source } returns StatusSource.ACTUAL
            every { customerId } returns id
            every { balance } returns PaymentAccountStatusValue.Balance(
                fiatBalance = PaymentAccountStatusValue.FiatBalance(
                    availableBalance = BigDecimal.ZERO,
                    currency = "USD",
                ),
                cryptoBalance = PaymentAccountStatusValue.CryptoBalance(
                    id = "id",
                    chainId = 1L,
                    depositAddress = "address",
                    tokenContractAddress = "contract",
                    balance = BigDecimal.ZERO,
                ),
                availableForWithdrawal = BigDecimal.ZERO,
            )
        }
    }
}