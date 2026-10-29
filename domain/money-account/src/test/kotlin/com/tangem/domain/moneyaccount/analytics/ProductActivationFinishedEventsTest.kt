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
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import kotlin.time.Duration.Companion.minutes

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ProductActivationFinishedEventsTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN final state WHEN events built THEN one event per finished ordered product`(model: StateModel) {
        // Act
        val actual = ProductActivationFinishedEvents.of(
            finalState = model.finalState,
            orderedProducts = model.orderedProducts,
            sinceOrder = SINCE_ORDER,
            plan = null,
        )

        // Assert
        assertThat(actual).containsExactlyElementsIn(model.expected).inOrder()
    }

    data class StateModel(
        val name: String,
        val finalState: TangemPayCustomerState?,
        val orderedProducts: Set<TangemPayProduct>,
        val expected: List<TangemPayAnalyticsEvents.ProductActivationFinished>,
    ) {
        override fun toString(): String = name
    }

    private fun provideTestModels() = listOf(
        StateModel(
            name = "both active",
            finalState = accounts(
                paymentAccount = PaymentAccountActivationState.Completed(cards = CARDS),
                moneyAccount = MoneyAccountActivationState.Completed,
            ),
            orderedProducts = BOTH,
            expected = listOf(event(Product.CARD, ActivationResult.ACTIVE), event(Product.ACCOUNT, ActivationResult.ACTIVE)),
        ),
        StateModel(
            name = "Plus not funded, account never started",
            finalState = accounts(
                paymentAccount = PaymentAccountActivationState.Failed(
                    reason = CustomerSummary.PaymentAccount.Activation.FailureReason.DEPOSIT_TIMEOUT,
                ),
                moneyAccount = MoneyAccountActivationState.NotStarted,
            ),
            orderedProducts = BOTH,
            expected = listOf(
                event(Product.CARD, ActivationResult.TIMED_OUT, ActivationFailureReason.PLUS_DEPOSIT_TIMEOUT),
            ),
        ),
        StateModel(
            name = "card rejected by the issuer, account failed technically",
            finalState = accounts(
                paymentAccount = PaymentAccountActivationState.Failed(
                    reason = CustomerSummary.PaymentAccount.Activation.FailureReason.RAIN_REJECTED,
                ),
                moneyAccount = MoneyAccountActivationState.Failed(
                    reason = CustomerSummary.MoneyAccount.Activation.FailureReason.TECHNICAL,
                ),
            ),
            orderedProducts = BOTH,
            expected = listOf(
                event(Product.CARD, ActivationResult.FAILED, ActivationFailureReason.ISSUER_REJECTED),
                event(Product.ACCOUNT, ActivationResult.FAILED, ActivationFailureReason.OTHER),
            ),
        ),
        StateModel(
            name = "card unavailable, account rejected by the issuer",
            finalState = accounts(
                paymentAccount = PaymentAccountActivationState.Unavailable,
                moneyAccount = MoneyAccountActivationState.Failed(
                    reason = CustomerSummary.MoneyAccount.Activation.FailureReason.RAIN_REJECTED,
                ),
            ),
            orderedProducts = BOTH,
            expected = listOf(
                event(Product.CARD, ActivationResult.FAILED, ActivationFailureReason.OTHER),
                event(Product.ACCOUNT, ActivationResult.FAILED, ActivationFailureReason.ISSUER_REJECTED),
            ),
        ),
        StateModel(
            name = "former customer with a closed account",
            finalState = TangemPayCustomerState.Former(
                paymentAccount = PaymentAccountActivationState.Completed(cards = CARDS),
                moneyAccount = MoneyAccountActivationState.Deactivated,
            ),
            orderedProducts = BOTH,
            expected = listOf(event(Product.CARD, ActivationResult.ACTIVE)),
        ),
        StateModel(
            name = "account not ordered",
            finalState = accounts(
                paymentAccount = PaymentAccountActivationState.Completed(cards = CARDS),
                moneyAccount = MoneyAccountActivationState.Completed,
            ),
            orderedProducts = setOf(TangemPayProduct.PAYMENT_ACCOUNT),
            expected = listOf(event(Product.CARD, ActivationResult.ACTIVE)),
        ),
        StateModel(
            name = "verification declined",
            finalState = TangemPayCustomerState.KycDeclined,
            orderedProducts = BOTH,
            expected = emptyList(),
        ),
        StateModel(
            name = "polling stopped",
            finalState = null,
            orderedProducts = BOTH,
            expected = listOf(
                event(Product.CARD, ActivationResult.POLLING_STOPPED),
                event(Product.ACCOUNT, ActivationResult.POLLING_STOPPED),
            ),
        ),
    )

    private fun accounts(
        paymentAccount: PaymentAccountActivationState,
        moneyAccount: MoneyAccountActivationState,
    ) = TangemPayCustomerState.Accounts(paymentAccount = paymentAccount, moneyAccount = moneyAccount)

    private fun event(
        product: Product,
        result: ActivationResult,
        failureReason: ActivationFailureReason? = null,
    ) = TangemPayAnalyticsEvents.ProductActivationFinished(
        product = product,
        result = result,
        failureReason = failureReason,
        sinceOrder = SINCE_ORDER,
        plan = null,
    )

    private companion object {
        val SINCE_ORDER = 2.minutes
        val CARDS = CustomerSummary.Cards(active = 1, frozen = 0, pending = 0)
        val BOTH = setOf(TangemPayProduct.PAYMENT_ACCOUNT, TangemPayProduct.BANKING_ACCOUNT)
    }
}