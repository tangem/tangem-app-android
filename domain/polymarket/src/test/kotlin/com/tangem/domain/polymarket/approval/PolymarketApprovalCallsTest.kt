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