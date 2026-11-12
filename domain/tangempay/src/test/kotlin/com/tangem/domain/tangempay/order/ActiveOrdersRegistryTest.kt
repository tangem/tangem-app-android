package com.tangem.domain.tangempay.order

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.OrderDetails
import com.tangem.domain.tangempay.model.OrderStep
import com.tangem.domain.tangempay.model.TangemPayOrder
import com.tangem.test.core.getEmittedValues
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class ActiveOrdersRegistryTest {

    private val registry = ActiveOrdersRegistry()

    @Test
    fun `GIVEN observed wallet WHEN order put and removed THEN emits the wallet orders on each change`() = runTest {
        // Arrange
        val emitted = getEmittedValues(registry.observe(WALLET_A))
        runCurrent()

        // Act
        registry.put(orderRef = ORDER_REF_A, snapshot = ORDER_A)
        runCurrent()
        registry.remove(ORDER_REF_A)
        runCurrent()

        // Assert
        assertThat(emitted).containsExactly(emptyList<TangemPayOrder>(), listOf(ORDER_A), emptyList<TangemPayOrder>())
            .inOrder()
    }

    @Test
    fun `GIVEN observed wallet WHEN another wallet order put THEN nothing new is emitted`() = runTest {
        // Arrange
        val emitted = getEmittedValues(registry.observe(WALLET_A))
        runCurrent()

        // Act
        registry.put(orderRef = OrderRef(userWalletId = WALLET_B, id = "order-b"), snapshot = order(id = "order-b"))
        runCurrent()

        // Assert
        assertThat(emitted).containsExactly(emptyList<TangemPayOrder>())
    }

    private companion object {
        val WALLET_A = UserWalletId("aa")
        val WALLET_B = UserWalletId("bb")
        val ORDER_REF_A = OrderRef(userWalletId = WALLET_A, id = "order-a")
        val ORDER_A = order(id = "order-a")

        fun order(id: String) = TangemPayOrder(
            id = id,
            status = TangemPayOrder.Status.PROCESSING,
            step = OrderStep.UNKNOWN,
            details = OrderDetails.Unknown,
        )
    }
}