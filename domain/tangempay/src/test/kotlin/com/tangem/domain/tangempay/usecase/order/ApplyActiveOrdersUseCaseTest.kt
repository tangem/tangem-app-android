package com.tangem.domain.tangempay.usecase.order

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.tangempay.TangemPayCard
import com.tangem.domain.models.tangempay.TangemPayCardFrozenState
import com.tangem.domain.models.tangempay.TangemPayCardState
import com.tangem.domain.models.tangempay.TangemPayCardType
import com.tangem.domain.tangempay.model.OrderDetails
import com.tangem.domain.tangempay.model.OrderStep
import com.tangem.domain.tangempay.model.TangemPayOrder
import com.tangem.domain.tangempay.order.ActiveOrdersRegistry
import com.tangem.domain.tangempay.order.OrderRef
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.getEmittedValues
import com.tangem.test.mock.MockAccounts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ApplyActiveOrdersUseCaseTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN active orders WHEN invoke THEN only cards changed by a frozen state order are Pending`(
        model: OverlayModel,
    ) = runTest {
        // Arrange
        val registry = ActiveOrdersRegistry()
        model.orders.forEachIndexed { index, order ->
            registry.put(orderRef = OrderRef(userWalletId = USER_WALLET_ID, id = "order-$index"), snapshot = order)
        }
        val useCase = ApplyActiveOrdersUseCase(activeOrdersRegistry = registry)

        // Act
        val actual = useCase(USER_WALLET_ID, flowOf(paymentStatus(FIRST_CARD, SECOND_CARD))).first()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN not loaded status and frozen state order WHEN invoke THEN status is unchanged`() = runTest {
        // Arrange
        val registry = ActiveOrdersRegistry()
        registry.put(orderRef = ORDER_REF, snapshot = order(OrderDetails.CardFreeze(FIRST_CARD.productInstanceId)))
        val useCase = ApplyActiveOrdersUseCase(activeOrdersRegistry = registry)
        val status = AccountStatus.Payment(
            account = MockAccounts.createPaymentAccountStatus().account,
            value = PaymentAccountStatusValue.Empty,
        )

        // Act
        val actual = useCase(USER_WALLET_ID, flowOf(status)).first()

        // Assert
        assertThat(actual).isEqualTo(status)
    }

    @Test
    fun `GIVEN frozen state order WHEN it is registered and removed THEN card is Pending only meanwhile`() = runTest {
        // Arrange
        val registry = ActiveOrdersRegistry()
        val useCase = ApplyActiveOrdersUseCase(activeOrdersRegistry = registry)
        val emitted = getEmittedValues(useCase(USER_WALLET_ID, flowOf(paymentStatus(FIRST_CARD))))
        runCurrent()

        // Act
        registry.put(orderRef = ORDER_REF, snapshot = order(OrderDetails.CardFreeze(FIRST_CARD.productInstanceId)))
        runCurrent()
        registry.remove(ORDER_REF)
        runCurrent()

        // Assert
        assertThat(emitted).containsExactly(
            paymentStatus(FIRST_CARD),
            paymentStatus(FIRST_CARD.pending()),
            paymentStatus(FIRST_CARD),
        ).inOrder()
    }

    @Test
    fun `GIVEN active frozen state order WHEN status updated THEN card stays Pending`() = runTest {
        // Arrange
        val registry = ActiveOrdersRegistry()
        registry.put(orderRef = ORDER_REF, snapshot = order(OrderDetails.CardFreeze(FIRST_CARD.productInstanceId)))
        val useCase = ApplyActiveOrdersUseCase(activeOrdersRegistry = registry)
        val statuses = MutableStateFlow(paymentStatus(FIRST_CARD))
        val emitted = getEmittedValues(useCase(USER_WALLET_ID, statuses))
        runCurrent()

        // Act
        statuses.value = paymentStatus(FIRST_CARD.copy(lastDigits = "9999"))
        runCurrent()

        // Assert
        assertThat(emitted.last()).isEqualTo(paymentStatus(FIRST_CARD.copy(lastDigits = "9999").pending()))
    }

    private fun provideTestModels() = listOf(
        OverlayModel(
            orders = emptyList(),
            expected = paymentStatus(FIRST_CARD, SECOND_CARD),
        ),
        OverlayModel(
            orders = listOf(order(OrderDetails.CardFreeze(productInstanceId = FIRST_CARD.productInstanceId))),
            expected = paymentStatus(FIRST_CARD.pending(), SECOND_CARD),
        ),
        OverlayModel(
            orders = listOf(order(OrderDetails.CardUnfreeze(productInstanceId = SECOND_CARD.productInstanceId))),
            expected = paymentStatus(FIRST_CARD, SECOND_CARD.pending()),
        ),
        OverlayModel(
            orders = listOf(
                order(OrderDetails.CardFreeze(productInstanceId = FIRST_CARD.productInstanceId)),
                order(OrderDetails.CardUnfreeze(productInstanceId = SECOND_CARD.productInstanceId)),
            ),
            expected = paymentStatus(FIRST_CARD.pending(), SECOND_CARD.pending()),
        ),
        OverlayModel(
            orders = listOf(order(OrderDetails.CardFreeze(productInstanceId = "pi-other"))),
            expected = paymentStatus(FIRST_CARD, SECOND_CARD),
        ),
        OverlayModel(
            orders = listOf(
                order(OrderDetails.CardClose(productInstanceId = FIRST_CARD.productInstanceId)),
                order(OrderDetails.Unknown),
            ),
            expected = paymentStatus(FIRST_CARD, SECOND_CARD),
        ),
    )

    internal data class OverlayModel(
        val orders: List<TangemPayOrder>,
        val expected: AccountStatus.Payment,
    )

    private fun paymentStatus(vararg cards: TangemPayCard): AccountStatus.Payment {
        val status = MockAccounts.createPaymentAccountStatus(userWalletId = USER_WALLET_ID)
        val loaded = status.value as PaymentAccountStatusValue.Loaded
        return status.copy(value = loaded.copy(cards = cards.toList()))
    }

    private fun TangemPayCard.pending() = copy(frozenState = TangemPayCardFrozenState.Pending)

    private fun order(details: OrderDetails) = TangemPayOrder(
        id = "order-1",
        status = TangemPayOrder.Status.PROCESSING,
        step = OrderStep.UNKNOWN,
        details = details,
    )

    private companion object {
        val USER_WALLET_ID = MockAccounts.userWalletId
        val ORDER_REF = OrderRef(userWalletId = USER_WALLET_ID, id = "order-1")
        val FIRST_CARD = card(id = "card-1", productInstanceId = "pi-1", frozenState = TangemPayCardFrozenState.Unfrozen)
        val SECOND_CARD = card(id = "card-2", productInstanceId = "pi-2", frozenState = TangemPayCardFrozenState.Frozen)

        fun card(id: String, productInstanceId: String, frozenState: TangemPayCardFrozenState) = TangemPayCard(
            id = id,
            productInstanceId = productInstanceId,
            cardStatus = TangemPayCard.Status.ACTIVE,
            hasPinCode = true,
            displayName = null,
            limit = null,
            frozenState = frozenState,
            lastDigits = "1234",
            images = emptyList(),
            state = TangemPayCardState.Active,
            embossName = null,
            cardType = TangemPayCardType.VIRTUAL,
        )
    }
}