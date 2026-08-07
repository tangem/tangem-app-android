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
    fun `GIVEN the batch WHEN build THEN collateral grants precede the share grants`() {
        // Act
        val calls = PolymarketApprovalCalls.build()

        // Assert
        assertThat(calls).hasSize(6)
        assertThat(calls.map { it.target }).containsExactly(
            collateral, collateral, collateral,
            conditionalTokens, conditionalTokens, conditionalTokens,
        ).inOrder()
        assertThat(calls.map { it.value }.toSet()).containsExactly("0")
    }

    @Test
    fun `GIVEN the deprecated CLOB v1 neg-risk adapter WHEN build THEN it is not a spender`() {
        // Act
        val calls = PolymarketApprovalCalls.build()

        // Assert
        assertThat(calls.none { it.data.contains(DEPRECATED_NEG_RISK_ADAPTER, ignoreCase = true) }).isTrue()
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
    fun `GIVEN the deposit-wallet factory WHEN read THEN equals the address Polymarket publishes`() {
        // Assert
        assertThat(PolymarketContracts.DW_FACTORY).isEqualTo("0x00000000000Fb5C9ADea0298D729A0CB3823Cc07")
    }

    internal data class CallModel(val index: Int, val target: String, val data: String)

    /** The set the backend's approvals validator accepts, spelled out rather than derived from the code. */
    private fun provideTestModels() = listOf(
        CallModel(
            index = 0, target = collateral,
            data = "0x095ea7b3" +
                "000000000000000000000000e111180000d2663c0091e4f400237545b87b996b" +
                "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
        ),
        CallModel(
            index = 1, target = collateral,
            data = "0x095ea7b3" +
                "000000000000000000000000e2222d279d744050d28e00520010520000310f59" +
                "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
        ),
        CallModel(
            index = 2, target = collateral,
            data = "0x095ea7b3" +
                "000000000000000000000000ada2005600dec949baf300f4c6120000bdb6eaab" +
                "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
        ),
        CallModel(
            index = 3, target = conditionalTokens,
            data = "0xa22cb465" +
                "000000000000000000000000e111180000d2663c0091e4f400237545b87b996b" +
                "0000000000000000000000000000000000000000000000000000000000000001",
        ),
        CallModel(
            index = 4, target = conditionalTokens,
            data = "0xa22cb465" +
                "000000000000000000000000e2222d279d744050d28e00520010520000310f59" +
                "0000000000000000000000000000000000000000000000000000000000000001",
        ),
        CallModel(
            index = 5, target = conditionalTokens,
            data = "0xa22cb465" +
                "000000000000000000000000ada2005600dec949baf300f4c6120000bdb6eaab" +
                "0000000000000000000000000000000000000000000000000000000000000001",
        ),
    )

    private companion object {
        const val DEPRECATED_NEG_RISK_ADAPTER = "d91e80cf2e7be2e162c6513ced06f1dd0da35296"
    }
}