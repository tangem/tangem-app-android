package com.tangem.domain.tangempay.order

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.tangempay.model.OrderDetails
import com.tangem.domain.tangempay.model.OrderStep
import com.tangem.domain.tangempay.model.TangemPayOrder
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OrderPollingStateTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN polling state WHEN read flags THEN terminal and completed match the order outcome`(model: StateModel) {
        // Act
        val actual = model.state.isTerminal to model.state.isCompleted

        // Assert
        assertThat(actual).isEqualTo(model.isTerminal to model.isCompleted)
    }

    private fun provideTestModels() = listOf(
        StateModel(state = OrderPollingState.Pending, isTerminal = false, isCompleted = false),
        StateModel(state = loaded(TangemPayOrder.Status.NEW), isTerminal = false, isCompleted = false),
        StateModel(state = loaded(TangemPayOrder.Status.PROCESSING), isTerminal = false, isCompleted = false),
        StateModel(state = loaded(TangemPayOrder.Status.UNKNOWN), isTerminal = false, isCompleted = false),
        StateModel(state = loaded(TangemPayOrder.Status.COMPLETED), isTerminal = true, isCompleted = true),
        StateModel(state = loaded(TangemPayOrder.Status.CANCELED), isTerminal = true, isCompleted = false),
        StateModel(state = OrderPollingState.NotFound, isTerminal = true, isCompleted = false),
        StateModel(state = OrderPollingState.Stopped, isTerminal = true, isCompleted = false),
    )

    private fun loaded(status: TangemPayOrder.Status) = OrderPollingState.Loaded(
        TangemPayOrder(
            id = "order-1",
            customerId = "customer-1",
            status = status,
            step = OrderStep.UNKNOWN,
            stepChangeCode = 0,
            createdAt = "2026-11-09T10:00:00Z",
            updatedAt = "2026-11-09T10:00:00Z",
            errorCode = null,
            details = OrderDetails.Unknown,
        ),
    )

    internal data class StateModel(val state: OrderPollingState, val isTerminal: Boolean, val isCompleted: Boolean)
}