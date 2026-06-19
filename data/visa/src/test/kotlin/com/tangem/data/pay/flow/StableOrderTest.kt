package com.tangem.data.pay.flow

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.pay.TangemPayCard
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.pay.TangemPayCardState
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StableOrderTest {

    @ParameterizedTest
    @ProvideTestModels
    fun stableOrder(model: Model) {
        // Act
        val result = model.cards.stableOrder(model.previousOrder).map { it.id }

        // Assert
        assertThat(result).containsExactlyElementsIn(model.expectedIds).inOrder()
    }

    private fun provideTestModels() = listOf(
        Model(
            name = "GIVEN no previous order WHEN stableOrder THEN backend order is kept",
            cards = listOf(card("a"), card("b"), card("c")),
            previousOrder = emptyList(),
            expectedIds = listOf("a", "b", "c"),
        ),
        Model(
            // The reported bug: a rename bumps updated_at, so the backend moves the renamed card.
            name = "GIVEN backend reordered existing cards WHEN stableOrder THEN previous order is preserved",
            cards = listOf(card("b"), card("c"), card("a")),
            previousOrder = listOf("a", "b", "c"),
            expectedIds = listOf("a", "b", "c"),
        ),
        Model(
            name = "GIVEN a newly seen card WHEN stableOrder THEN it is appended at the end",
            cards = listOf(card("c"), card("a"), card("b")),
            previousOrder = listOf("a", "b"),
            expectedIds = listOf("a", "b", "c"),
        ),
        Model(
            name = "GIVEN several new cards WHEN stableOrder THEN they keep their backend order at the end",
            cards = listOf(card("d"), card("b"), card("a"), card("c")),
            previousOrder = listOf("a", "b"),
            expectedIds = listOf("a", "b", "d", "c"),
        ),
        Model(
            name = "GIVEN previous order references a gone card WHEN stableOrder THEN the stale id is ignored",
            cards = listOf(card("b"), card("a")),
            previousOrder = listOf("a", "x", "b"),
            expectedIds = listOf("a", "b"),
        ),
    )

    internal data class Model(
        val name: String,
        val cards: List<TangemPayCard>,
        val previousOrder: List<String>,
        val expectedIds: List<String>,
    ) {
        override fun toString(): String = name
    }

    private companion object {
        fun card(id: String): TangemPayCard = TangemPayCard(
            id = id,
            productInstanceId = id,
            cardStatus = TangemPayCard.Status.ACTIVE,
            hasPinCode = false,
            displayName = null,
            limit = null,
            frozenState = TangemPayCardFrozenState.Unfrozen,
            lastDigits = "0000",
            state = TangemPayCardState.Active,
        )
    }
}