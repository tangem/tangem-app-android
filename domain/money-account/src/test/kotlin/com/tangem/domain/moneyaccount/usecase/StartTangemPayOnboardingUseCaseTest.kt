package com.tangem.domain.moneyaccount.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.account.TangemPayTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlanTransition
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.model.MoneyAccountActivationError
import com.tangem.domain.moneyaccount.model.StartTangemPayOnboardingError
import com.tangem.domain.pay.model.CustomerSummary
import com.tangem.domain.pay.model.MoneyAccountActivationState
import com.tangem.domain.pay.model.PaymentAccountActivationState
import com.tangem.domain.pay.model.TangemPayCustomerState
import com.tangem.domain.pay.usecase.CreateOnboardingOrderUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents.ActivationFailureReason
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents.ActivationResult
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents.SignatureResult
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents.WalletType
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.domain.visa.model.MoneyAccountDepositCredentials
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StartTangemPayOnboardingUseCaseTest {

    private val activateMoneyAccountUseCase: ActivateMoneyAccountUseCase = mockk()
    private val createOnboardingOrderUseCase: CreateOnboardingOrderUseCase = mockk()
    private val pollTangemPayAccountStatusesUseCase: PollTangemPayAccountStatusesUseCase = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val analytics: AnalyticsEventHandler = mockk()
    private val sentEvents = mutableListOf<AnalyticsEvent>()

    private val useCase = StartTangemPayOnboardingUseCase(
        activateMoneyAccountUseCase = activateMoneyAccountUseCase,
        createOnboardingOrderUseCase = createOnboardingOrderUseCase,
        pollTangemPayAccountStatusesUseCase = pollTangemPayAccountStatusesUseCase,
        userWalletsListRepository = userWalletsListRepository,
        analytics = analytics,
        appCoroutineScope = TestAppCoroutineScope(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(activateMoneyAccountUseCase, createOnboardingOrderUseCase, pollTangemPayAccountStatusesUseCase)
        clearMocks(userWalletsListRepository, analytics)
        sentEvents.clear()
        every { pollTangemPayAccountStatusesUseCase(any()) } returns emptyFlow()
        coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(wallet<UserWallet.Cold>())
        every { analytics.send(capture(sentEvents)) } just Runs
    }

    @Test
    fun `GIVEN banking account available WHEN invoke THEN creates order with credentials and starts polling`() =
        runTest {
            // Arrange
            coEvery { activateMoneyAccountUseCase(USER_WALLET_ID) } returns CREDENTIALS.right()
            coEvery { createOnboardingOrderUseCase(any(), any(), any()) } returns Unit.right()

            // Act
            val actual = useCase(
                userWalletId = USER_WALLET_ID,
                tariffPlanTransition = TRANSITION,
                availableProducts = setOf(TangemPayProduct.BANKING_ACCOUNT),
            )

            // Assert
            assertThat(actual).isEqualTo(Unit.right())
            coVerify(exactly = 1) {
                createOnboardingOrderUseCase(
                    userWalletId = USER_WALLET_ID,
                    tariffPlanTransition = TRANSITION,
                    depositCredentials = CREDENTIALS,
                )
            }
            verify(exactly = 1) { pollTangemPayAccountStatusesUseCase(USER_WALLET_ID) }
        }

    @Test
    fun `GIVEN banking account unavailable WHEN invoke THEN creates order without activation and starts polling`() =
        runTest {
            // Arrange
            coEvery { createOnboardingOrderUseCase(any(), any(), any()) } returns Unit.right()

            // Act
            val actual = useCase(
                userWalletId = USER_WALLET_ID,
                tariffPlanTransition = TRANSITION,
                availableProducts = emptySet(),
            )

            // Assert
            assertThat(actual).isEqualTo(Unit.right())
            coVerify(exactly = 0) { activateMoneyAccountUseCase(any()) }
            coVerify(exactly = 1) {
                createOnboardingOrderUseCase(
                    userWalletId = USER_WALLET_ID,
                    tariffPlanTransition = TRANSITION,
                    depositCredentials = null,
                )
            }
            verify(exactly = 1) { pollTangemPayAccountStatusesUseCase(USER_WALLET_ID) }
        }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN activation fails WHEN invoke THEN signature reported AND order skipped with mapped error`(
        model: ActivationErrorModel,
    ) =
        runTest {
            // Arrange
            coEvery { activateMoneyAccountUseCase(USER_WALLET_ID) } returns model.activationError.left()

            // Act
            val actual = useCase(
                userWalletId = USER_WALLET_ID,
                tariffPlanTransition = TRANSITION,
                availableProducts = setOf(TangemPayProduct.BANKING_ACCOUNT),
            )

            // Assert
            assertThat(actual).isEqualTo(model.expected)
            assertThat(sentEvents).containsExactly(
                TangemPayAnalyticsEvents.WalletSignatureRequested(walletType = WalletType.CARD),
                TangemPayAnalyticsEvents.WalletSignatureFinished(
                    result = model.expectedSignatureResult,
                    walletType = WalletType.CARD,
                ),
            ).inOrder()
            coVerify(exactly = 0) { createOnboardingOrderUseCase(any(), any(), any()) }
            verify(exactly = 0) { pollTangemPayAccountStatusesUseCase(any()) }
        }

    @Test
    fun `GIVEN banking account available WHEN order created THEN signature and order created events sent`() =
        runTest {
            // Arrange
            coEvery { activateMoneyAccountUseCase(USER_WALLET_ID) } returns CREDENTIALS.right()
            coEvery { createOnboardingOrderUseCase(any(), any(), any()) } returns Unit.right()

            // Act
            useCase(
                userWalletId = USER_WALLET_ID,
                tariffPlanTransition = TRANSITION,
                availableProducts = setOf(TangemPayProduct.BANKING_ACCOUNT),
            )

            // Assert
            val orderEvents = sentEvents.filterNot { it is TangemPayAnalyticsEvents.ProductActivationFinished }
            assertThat(orderEvents).containsExactly(
                TangemPayAnalyticsEvents.WalletSignatureRequested(walletType = WalletType.CARD),
                TangemPayAnalyticsEvents.WalletSignatureFinished(
                    result = SignatureResult.SIGNED,
                    walletType = WalletType.CARD,
                ),
                TangemPayAnalyticsEvents.OnboardingOrderCreated(
                    availableProducts = setOf(TangemPayProduct.BANKING_ACCOUNT),
                    plan = TRANSITION.plan,
                ),
            ).inOrder()
        }

    @Test
    fun `GIVEN order created WHEN products finish at different times THEN each reported when it finished`() =
        runTest {
            // Arrange
            coEvery { activateMoneyAccountUseCase(USER_WALLET_ID) } returns CREDENTIALS.right()
            coEvery { createOnboardingOrderUseCase(any(), any(), any()) } returns Unit.right()
            every { pollTangemPayAccountStatusesUseCase(USER_WALLET_ID) } returns flow {
                delay(30.seconds)
                emit(accounts(paymentAccount = CARD_ISSUING, moneyAccount = MoneyAccountActivationState.Completed))
                delay(6.minutes)
                emit(accounts(paymentAccount = CARD_REJECTED, moneyAccount = MoneyAccountActivationState.Completed))
            }
            val useCase = createUseCase()

            // Act
            useCase(
                userWalletId = USER_WALLET_ID,
                tariffPlanTransition = TRANSITION,
                availableProducts = setOf(TangemPayProduct.PAYMENT_ACCOUNT, TangemPayProduct.BANKING_ACCOUNT),
            )
            advanceUntilIdle()

            // Assert
            assertThat(sentEvents.filterIsInstance<TangemPayAnalyticsEvents.ProductActivationFinished>())
                .containsExactly(
                    TangemPayAnalyticsEvents.ProductActivationFinished(
                        product = TangemPayAnalyticsEvents.Product.ACCOUNT,
                        result = ActivationResult.ACTIVE,
                        failureReason = null,
                        sinceOrder = 30.seconds,
                        plan = TRANSITION.plan,
                    ),
                    TangemPayAnalyticsEvents.ProductActivationFinished(
                        product = TangemPayAnalyticsEvents.Product.CARD,
                        result = ActivationResult.FAILED,
                        failureReason = ActivationFailureReason.ISSUER_REJECTED,
                        sinceOrder = 6.minutes + 30.seconds,
                        plan = TRANSITION.plan,
                    ),
                )
                .inOrder()
        }

    @Test
    fun `GIVEN account active after 40 minutes WHEN polling ends THEN late result kept and card polling stopped`() =
        runTest {
            // Arrange
            coEvery { activateMoneyAccountUseCase(USER_WALLET_ID) } returns CREDENTIALS.right()
            coEvery { createOnboardingOrderUseCase(any(), any(), any()) } returns Unit.right()
            every { pollTangemPayAccountStatusesUseCase(USER_WALLET_ID) } returns flow {
                delay(40.minutes)
                emit(accounts(paymentAccount = CARD_ISSUING, moneyAccount = MoneyAccountActivationState.Completed))
            }
            val useCase = createUseCase()

            // Act
            useCase(
                userWalletId = USER_WALLET_ID,
                tariffPlanTransition = null,
                availableProducts = setOf(TangemPayProduct.PAYMENT_ACCOUNT, TangemPayProduct.BANKING_ACCOUNT),
            )
            advanceUntilIdle()

            // Assert
            assertThat(sentEvents.filterIsInstance<TangemPayAnalyticsEvents.ProductActivationFinished>())
                .containsExactly(
                    TangemPayAnalyticsEvents.ProductActivationFinished(
                        product = TangemPayAnalyticsEvents.Product.ACCOUNT,
                        result = ActivationResult.ACTIVE,
                        failureReason = null,
                        sinceOrder = 40.minutes,
                        plan = null,
                    ),
                    TangemPayAnalyticsEvents.ProductActivationFinished(
                        product = TangemPayAnalyticsEvents.Product.CARD,
                        result = ActivationResult.POLLING_STOPPED,
                        failureReason = null,
                        sinceOrder = 40.minutes,
                        plan = null,
                    ),
                )
                .inOrder()
        }

    @Test
    fun `GIVEN hot wallet WHEN signature requested THEN hot wallet type reported`() = runTest {
        // Arrange
        coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(wallet<UserWallet.Hot>())
        coEvery { activateMoneyAccountUseCase(USER_WALLET_ID) } returns MoneyAccountActivationError.UserCancelled.left()

        // Act
        useCase(
            userWalletId = USER_WALLET_ID,
            tariffPlanTransition = TRANSITION,
            availableProducts = setOf(TangemPayProduct.BANKING_ACCOUNT),
        )

        // Assert
        assertThat(sentEvents).containsExactly(
            TangemPayAnalyticsEvents.WalletSignatureRequested(walletType = WalletType.HOT),
            TangemPayAnalyticsEvents.WalletSignatureFinished(
                result = SignatureResult.CANCELLED,
                walletType = WalletType.HOT,
            ),
        ).inOrder()
    }

    @Test
    fun `GIVEN card only and order creation fails WHEN invoke THEN only order failed event sent`() = runTest {
        // Arrange
        coEvery {
            createOnboardingOrderUseCase(any(), any(), any())
        } returns VisaApiError.CardIssueActiveOrderExists.left()

        // Act
        useCase(
            userWalletId = USER_WALLET_ID,
            tariffPlanTransition = null,
            availableProducts = setOf(TangemPayProduct.PAYMENT_ACCOUNT),
        )

        // Assert
        assertThat(sentEvents).containsExactly(
            TangemPayAnalyticsEvents.OnboardingOrderFailed(
                error = VisaApiError.CardIssueActiveOrderExists,
                availableProducts = setOf(TangemPayProduct.PAYMENT_ACCOUNT),
            ),
        )
    }

    @Test
    fun `GIVEN order creation fails WHEN invoke THEN returns failed and skips polling`() = runTest {
        // Arrange
        coEvery {
            createOnboardingOrderUseCase(any(), any(), any())
        } returns VisaApiError.Unspecified.left()

        // Act
        val actual = useCase(
            userWalletId = USER_WALLET_ID,
            tariffPlanTransition = null,
            availableProducts = emptySet(),
        )

        // Assert
        assertThat(actual).isEqualTo(StartTangemPayOnboardingError.Failed.left())
        verify(exactly = 0) { pollTangemPayAccountStatusesUseCase(any()) }
    }

    private fun provideTestModels() = listOf(
        ActivationErrorModel(
            activationError = MoneyAccountActivationError.UserCancelled,
            expected = StartTangemPayOnboardingError.UserCancelled.left(),
            expectedSignatureResult = SignatureResult.CANCELLED,
        ),
        ActivationErrorModel(
            activationError = MoneyAccountActivationError.PersistenceFailed,
            expected = StartTangemPayOnboardingError.Failed.left(),
            expectedSignatureResult = SignatureResult.SIGNED,
        ),
        ActivationErrorModel(
            activationError = MoneyAccountActivationError.Unknown,
            expected = StartTangemPayOnboardingError.Failed.left(),
            expectedSignatureResult = SignatureResult.FAILED,
        ),
    )

    internal data class ActivationErrorModel(
        val activationError: MoneyAccountActivationError,
        val expected: Either<StartTangemPayOnboardingError, Unit>,
        val expectedSignatureResult: SignatureResult,
    )

    private fun TestScope.createUseCase() = StartTangemPayOnboardingUseCase(
        activateMoneyAccountUseCase = activateMoneyAccountUseCase,
        createOnboardingOrderUseCase = createOnboardingOrderUseCase,
        pollTangemPayAccountStatusesUseCase = pollTangemPayAccountStatusesUseCase,
        userWalletsListRepository = userWalletsListRepository,
        analytics = analytics,
        appCoroutineScope = TestAppCoroutineScope(this),
        timeSource = testScheduler.timeSource,
    )

    private inline fun <reified T : UserWallet> wallet(): T = mockk {
        every { walletId } returns USER_WALLET_ID
    }

    private fun accounts(
        paymentAccount: PaymentAccountActivationState,
        moneyAccount: MoneyAccountActivationState,
    ) = TangemPayCustomerState.Accounts(paymentAccount = paymentAccount, moneyAccount = moneyAccount)

    private companion object {
        val USER_WALLET_ID = UserWalletId("aabbcc112233")

        val TRANSITION = TangemPayTariffPlanTransition(
            type = TangemPayTariffPlanTransition.Type.ACTIVATION,
            plan = TangemPayTariffPlan(
                id = "plan-plus",
                tierId = "PLUS",
                isBasicTier = false,
                name = "Plus",
                programName = "program-plus",
                descriptionItems = emptyList(),
            ),
        )

        val CREDENTIALS = MoneyAccountDepositCredentials(address = "0xdeposit", signature = "0xsig")

        val CARD_ISSUING = PaymentAccountActivationState.Issuing(step = PaymentAccountActivationState.Issuing.Step.ISSUING)
        val CARD_REJECTED = PaymentAccountActivationState.Failed(
            reason = CustomerSummary.PaymentAccount.Activation.FailureReason.RAIN_REJECTED,
        )
    }
}