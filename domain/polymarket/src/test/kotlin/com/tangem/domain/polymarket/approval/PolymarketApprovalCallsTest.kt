package com.tangem.domain.polymarket.approval

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketApprovalCallsTest {

    private val collateral = "0xC011a7E12a19f7B1f670d46F03B03f3342E82DFB"
    private val conditionalTokens = "0x4D97DCd97eC945f40cF65F87097ACe5EA0476045"

    @Test
    fun `GIVEN Polygon network WHEN read CHAIN_ID THEN it is 137`() {
        assertThat(PolymarketContracts.CHAIN_ID).isEqualTo(137L)
    }

    @Test
    fun `GIVEN canonical batch WHEN build THEN 6 calls in documented order`() {
        // Act
        val calls = PolymarketApprovalCalls.build()

        // Assert
        assertThat(calls).hasSize(6)
        assertThat(calls.map { it.target }).containsExactly(
            collateral, conditionalTokens, collateral, conditionalTokens, collateral, conditionalTokens,
        ).inOrder()
        assertThat(calls.map { it.value }.toSet()).containsExactly("0")
    }

    @ParameterizedTest
    @MethodSource("provideTestModels")
    fun `GIVEN index WHEN build THEN call data is byte-exact (Appendix D)`(model: CallModel) {
        // Act
        val call = PolymarketApprovalCalls.build()[model.index]

        // Assert
        assertThat(call.target).isEqualTo(model.target)
        assertThat(call.value).isEqualTo("0")
        assertThat(call.data).isEqualTo(model.data)
    }

    @Test
    fun `GIVEN CREATE2 constants WHEN read THEN equal the Polymarket reference values`() {
        // Assert
        assertThat(PolymarketContracts.DW_FACTORY).isEqualTo("0x00000000000Fb5C9ADea0298D729A0CB3823Cc07")
        assertThat(PolymarketContracts.DW_IMPLEMENTATION).isEqualTo("0x58CA52ebe0DadfdF531Cde7062e76746de4Db1eB")
        assertThat(PolymarketContracts.UUPS_INIT_CONST1)
            .isEqualTo("0xcc3735a920a3ca505d382bbc545af43d6000803e6038573d6000fd5b3d6000f3")
        assertThat(PolymarketContracts.UUPS_INIT_CONST2)
            .isEqualTo("0x5155f3363d3d373d3d363d7f360894a13ba1a3210667c828492db98dca3e2076")
    }

    internal data class CallModel(val index: Int, val target: String, val data: String)

    private fun provideTestModels() = listOf(
        CallModel(
            index = 0, target = collateral,
            data = "0x095ea7b3" +
                "000000000000000000000000e111180000d2663c0091e4f400237545b87b996b" +
                "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
        ),
        CallModel(
            index = 1, target = conditionalTokens,
            data = "0xa22cb465" +
                "000000000000000000000000e111180000d2663c0091e4f400237545b87b996b" +
                "0000000000000000000000000000000000000000000000000000000000000001",
        ),
        CallModel(
            index = 2, target = collateral,
            data = "0x095ea7b3" +
                "000000000000000000000000e2222d279d744050d28e00520010520000310f59" +
                "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
        ),
        CallModel(
            index = 3, target = conditionalTokens,
            data = "0xa22cb465" +
                "000000000000000000000000e2222d279d744050d28e00520010520000310f59" +
                "0000000000000000000000000000000000000000000000000000000000000001",
        ),
        CallModel(
            index = 4, target = collateral,
            data = "0x095ea7b3" +
                "000000000000000000000000d91e80cf2e7be2e162c6513ced06f1dd0da35296" +
                "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
        ),
        CallModel(
            index = 5, target = conditionalTokens,
            data = "0xa22cb465" +
                "000000000000000000000000d91e80cf2e7be2e162c6513ced06f1dd0da35296" +
                "0000000000000000000000000000000000000000000000000000000000000001",
        ),
    )
}