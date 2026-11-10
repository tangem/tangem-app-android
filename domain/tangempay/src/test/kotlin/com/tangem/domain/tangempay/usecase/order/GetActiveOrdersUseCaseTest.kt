package com.tangem.domain.tangempay.usecase.order

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.OrderDetails
import com.tangem.domain.tangempay.model.OrderStep
import com.tangem.domain.tangempay.model.TangemPayOrder
import com.tangem.domain.tangempay.order.ActiveOrdersRegistry
import com.tangem.domain.tangempay.order.OrderRef
import org.junit.jupiter.api.Test

internal class GetActiveOrdersUseCaseTest {

    private val activeOrdersRegistry = ActiveOrdersRegistry()
    private val useCase = GetActiveOrdersUseCase(activeOrdersRegistry = activeOrdersRegistry)

    @Test
    fun `GIVEN no active orders WHEN invoke THEN returns empty list`() {
        // Act
        val actual = useCase(WALLET_A)

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN active orders of two wallets WHEN invoke THEN returns only the requested wallet orders`() {
        // Arrange
        val walletAOrder = order(id = "order-a")
        activeOrdersRegistry.put(orderRef = OrderRef(userWalletId = WALLET_A, id = "order-a"), snapshot = walletAOrder)
        activeOrdersRegistry.put(
            orderRef = OrderRef(userWalletId = WALLET_B, id = "order-b"),
            snapshot = order(id = "order-b"),
        )

        // Act
        val actual = useCase(WALLET_A)

        // Assert
        assertThat(actual).containsExactly(walletAOrder)
    }

    @Test
    fun `GIVEN removed order WHEN invoke THEN it is not returned`() {
        // Arrange
        val orderRef = OrderRef(userWalletId = WALLET_A, id = "order-a")
        activeOrdersRegistry.put(orderRef = orderRef, snapshot = order(id = "order-a"))
        activeOrdersRegistry.remove(orderRef)

        // Act
        val actual = useCase(WALLET_A)

        // Assert
        assertThat(actual).isEmpty()
    }

    private fun order(id: String) = TangemPayOrder(
        id = id,
        status = TangemPayOrder.Status.PROCESSING,
        step = OrderStep.UNKNOWN,
        details = OrderDetails.CardFreeze(productInstanceId = "pi-1"),
    )

    private companion object {
        val WALLET_A = UserWalletId("aabbcc")
        val WALLET_B = UserWalletId("ddeeff")
    }
}