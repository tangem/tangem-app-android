package com.tangem.features.yield.supply.impl.entry

import com.google.common.truth.Truth.assertThat
import com.tangem.features.yield.supply.api.entry.YieldSupplyEntryRoute
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.yield.MockYieldSupply
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class YieldSupplyEntryBackTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN entry back stack WHEN back from the active screen THEN leaves the flow only at its root`(
        model: BackModel,
    ) {
        // Act
        val actual = model.backStack.isFlowRootOnBack()

        // Assert
        assertThat(actual).isEqualTo(model.isFlowRoot)
    }

    private fun provideTestModels() = listOf(
        BackModel(name = "no back stack", backStack = emptyList(), isFlowRoot = true),
        BackModel(
            name = "only the initial empty route",
            backStack = listOf(YieldSupplyEntryRoute.Empty),
            isFlowRoot = true,
        ),
        BackModel(
            name = "a promo step underneath",
            backStack = listOf(YieldSupplyEntryRoute.Promo(cryptoCurrency = MockYieldSupply.token(), apy = "5")),
            isFlowRoot = false,
        ),
    )

    data class BackModel(val name: String, val backStack: List<YieldSupplyEntryRoute>, val isFlowRoot: Boolean) {
        override fun toString(): String = name
    }
}