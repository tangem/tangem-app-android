package com.tangem.domain.moneyaccount.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.pay.model.CustomerSummary
import com.tangem.domain.pay.model.MoneyAccountActivationState
import com.tangem.domain.pay.model.PaymentAccountActivationState
import com.tangem.domain.pay.model.TangemPayCustomerState
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents.ActivationFailureReason
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents.ActivationResult
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents.Product
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource
import kotlin.time.TimeMark

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ProductActivationTrackerTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN polled state WHEN polling stops THEN every ordered product reported once`(model: StateModel) {
        // Arrange
        val tracker = tracker(orderedProducts = model.orderedProducts)

        // Act
        val onState = tracker.onState(model.state)
        val onStop = tracker.onPollingStopped()

        // Assert
        assertThat(onState).containsExactlyElementsIn(model.expectedOnState).inOrder()
        assertThat(onStop).containsExactlyElementsIn(model.expectedOnStop).inOrder()
    }

    @Test
    fun `GIVEN products already reported WHEN same state polled again THEN nothing reported twice`() {
        // Arrange
        val tracker = tracker(orderedProducts = BOTH)
        tracker.onState(BOTH_ACTIVE)

        // Act
        val actual = tracker.onState(BOTH_ACTIVE) + tracker.onPollingStopped()

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN account active before card WHEN polling stops THEN each event carries its own time since order`() {
        // Arrange
        val timeSource = TestTimeSource()
        val tracker = tracker(orderedProducts = BOTH, orderedAt = timeSource.markNow())
        timeSource += 30.seconds

        // Act
        val accountEvents = tracker.onState(
            accounts(
                paymentAccount = PaymentAccountActivationState.Issuing(
                    step = PaymentAccountActivationState.Issuing.Step.AWAITING_DEPOSIT,
                ),
                moneyAccount = MoneyAccountActivationState.Completed,
            ),
        )
        timeSource += 15.minutes
        val cardEvents = tracker.onPollingStopped()

        // Assert
        assertThat(accountEvents).containsExactly(
            event(Product.ACCOUNT, ActivationResult.ACTIVE, sinceOrder = 30.seconds),
        )
        assertThat(cardEvents).containsExactly(
            event(Product.CARD, ActivationResult.POLLING_STOPPED, sinceOrder = 15.minutes + 30.seconds),
        )
    }

    data class StateModel(
        val name: String,
        val state: TangemPayCustomerState,
        val orderedProducts: Set<TangemPayProduct>,
        val expectedOnState: List<TangemPayAnalyticsEvents.ProductActivationFinished>,
        val expectedOnStop: List<TangemPayAnalyticsEvents.ProductActivationFinished> = emptyList(),
    ) {
        override fun toString(): String = name
    }

    private fun provideTestModels() = listOf(
        StateModel(
            name = "both active",
            state = BOTH_ACTIVE,
            orderedProducts = BOTH,
            expectedOnState = listOf(
                event(Product.CARD, ActivationResult.ACTIVE),
                event(Product.ACCOUNT, ActivationResult.ACTIVE),
            ),
        ),
        StateModel(
            name = "Plus not funded, account never started",
            state = accounts(
                paymentAccount = PaymentAccountActivationState.Failed(
                    reason = CustomerSummary.PaymentAccount.Activation.FailureReason.DEPOSIT_TIMEOUT,
                ),
                moneyAccount = MoneyAccountActivationState.NotStarted,
            ),
            orderedProducts = BOTH,
            expectedOnState = listOf(
                event(Product.CARD, ActivationResult.TIMED_OUT, ActivationFailureReason.PLUS_DEPOSIT_TIMEOUT),
            ),
            expectedOnStop = listOf(event(Product.ACCOUNT, ActivationResult.POLLING_STOPPED)),
        ),
        StateModel(
            name = "card rejected by the issuer, account failed technically",
            state = accounts(
                paymentAccount = PaymentAccountActivationState.Failed(
                    reason = CustomerSummary.PaymentAccount.Activation.FailureReason.RAIN_REJECTED,
                ),
                moneyAccount = MoneyAccountActivationState.Failed(
                    reason = CustomerSummary.MoneyAccount.Activation.FailureReason.TECHNICAL,
                ),
            ),
            orderedProducts = BOTH,
            expectedOnState = listOf(
                event(Product.CARD, ActivationResult.FAILED, ActivationFailureReason.ISSUER_REJECTED),
                event(Product.ACCOUNT, ActivationResult.FAILED, ActivationFailureReason.OTHER),
            ),
        ),
        StateModel(
            name = "card unavailable, account rejected by the issuer",
            state = accounts(
                paymentAccount = PaymentAccountActivationState.Unavailable,
                moneyAccount = MoneyAccountActivationState.Failed(
                    reason = CustomerSummary.MoneyAccount.Activation.FailureReason.RAIN_REJECTED,
                ),
            ),
            orderedProducts = BOTH,
            expectedOnState = listOf(
                event(Product.CARD, ActivationResult.FAILED, ActivationFailureReason.OTHER),
                event(Product.ACCOUNT, ActivationResult.FAILED, ActivationFailureReason.ISSUER_REJECTED),
            ),
        ),
        StateModel(
            name = "card active, account waiting on a declined verification",
            state = accounts(
                paymentAccount = PaymentAccountActivationState.Completed(cards = CARDS),
                moneyAccount = MoneyAccountActivationState.AwaitingKyc(kycStatus = CustomerSummary.KycStatus.DECLINED),
            ),
            orderedProducts = BOTH,
            expectedOnState = listOf(
                event(Product.CARD, ActivationResult.ACTIVE),
                event(Product.ACCOUNT, ActivationResult.FAILED, ActivationFailureReason.OTHER),
            ),
        ),
        StateModel(
            name = "former customer with a closed account",
            state = TangemPayCustomerState.Former(
                paymentAccount = PaymentAccountActivationState.Completed(cards = CARDS),
                moneyAccount = MoneyAccountActivationState.Deactivated,
            ),
            orderedProducts = BOTH,
            expectedOnState = listOf(event(Product.CARD, ActivationResult.ACTIVE)),
            expectedOnStop = listOf(event(Product.ACCOUNT, ActivationResult.POLLING_STOPPED)),
        ),
        StateModel(
            name = "account not ordered",
            state = BOTH_ACTIVE,
            orderedProducts = setOf(TangemPayProduct.PAYMENT_ACCOUNT),
            expectedOnState = listOf(event(Product.CARD, ActivationResult.ACTIVE)),
        ),
        StateModel(
            name = "verification declined",
            state = TangemPayCustomerState.KycDeclined,
            orderedProducts = BOTH,
            expectedOnState = listOf(
                event(Product.CARD, ActivationResult.FAILED, ActivationFailureReason.OTHER),
                event(Product.ACCOUNT, ActivationResult.FAILED, ActivationFailureReason.OTHER),
            ),
        ),
        StateModel(
            name = "customer blocked",
            state = TangemPayCustomerState.Blocked,
            orderedProducts = BOTH,
            expectedOnState = listOf(
                event(Product.CARD, ActivationResult.FAILED, ActivationFailureReason.OTHER),
                event(Product.ACCOUNT, ActivationResult.FAILED, ActivationFailureReason.OTHER),
            ),
        ),
        StateModel(
            name = "verification not finished",
            state = TangemPayCustomerState.KycRequired,
            orderedProducts = BOTH,
            expectedOnState = emptyList(),
            expectedOnStop = listOf(
                event(Product.CARD, ActivationResult.POLLING_STOPPED),
                event(Product.ACCOUNT, ActivationResult.POLLING_STOPPED),
            ),
        ),
        StateModel(
            name = "both still issuing",
            state = accounts(
                paymentAccount = PaymentAccountActivationState.Issuing(
                    step = PaymentAccountActivationState.Issuing.Step.ISSUING,
                ),
                moneyAccount = MoneyAccountActivationState.Issuing(
                    step = MoneyAccountActivationState.Issuing.Step.ISSUING,
                ),
            ),
            orderedProducts = BOTH,
            expectedOnState = emptyList(),
            expectedOnStop = listOf(
                event(Product.CARD, ActivationResult.POLLING_STOPPED),
                event(Product.ACCOUNT, ActivationResult.POLLING_STOPPED),
            ),
        ),
    )

    private fun tracker(
        orderedProducts: Set<TangemPayProduct>,
        orderedAt: TimeMark = TestTimeSource().markNow(),
    ) = ProductActivationTracker(orderedProducts = orderedProducts, plan = null, orderedAt = orderedAt)

    private fun event(
        product: Product,
        result: ActivationResult,
        failureReason: ActivationFailureReason? = null,
        sinceOrder: Duration = Duration.ZERO,
    ) = TangemPayAnalyticsEvents.ProductActivationFinished(
        product = product,
        result = result,
        failureReason = failureReason,
        sinceOrder = sinceOrder,
        plan = null,
    )

    private companion object {
        val CARDS = CustomerSummary.Cards(active = 1, frozen = 0, pending = 0)
        val BOTH = setOf(TangemPayProduct.PAYMENT_ACCOUNT, TangemPayProduct.BANKING_ACCOUNT)
        val BOTH_ACTIVE = TangemPayCustomerState.Accounts(
            paymentAccount = PaymentAccountActivationState.Completed(cards = CARDS),
            moneyAccount = MoneyAccountActivationState.Completed,
        )

        fun accounts(
            paymentAccount: PaymentAccountActivationState,
            moneyAccount: MoneyAccountActivationState,
        ) = TangemPayCustomerState.Accounts(paymentAccount = paymentAccount, moneyAccount = moneyAccount)
    }
}