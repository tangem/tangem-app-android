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
    fun `GIVEN the batch WHEN build THEN it is the 13 calls the validator expects`() {
        // Act
        val calls = PolymarketApprovalCalls.build()

        // Assert
        assertThat(calls).hasSize(EXPECTED_CALL_COUNT)
        assertThat(calls.count { it.target == collateral }).isEqualTo(EXPECTED_APPROVE_COUNT)
        assertThat(calls.count { it.target == conditionalTokens }).isEqualTo(EXPECTED_SET_APPROVAL_COUNT)
        assertThat(calls.map { it.value }.toSet()).containsExactly("0")
    }

    @Test
    fun `GIVEN the batch WHEN build THEN every spender is granted at most once per allowance kind`() {
        // Act
        val calls = PolymarketApprovalCalls.build()

        // Assert
        assertThat(calls.map { it.target to it.data }.toSet()).hasSize(EXPECTED_CALL_COUNT)
    }

    @Test
    fun `GIVEN the CLOB v1 neg-risk adapter WHEN build THEN it is a spender again`() {
        // Act
        val calls = PolymarketApprovalCalls.build()

        // Assert
        assertThat(calls.count { it.data.contains(NEG_RISK_ADAPTER, ignoreCase = true) })
            .isEqualTo(BOTH_ALLOWANCE_KINDS)
    }

    @Test
    fun `GIVEN the auto-redeem contract WHEN build THEN it is not a spender`() {
        // Act
        val calls = PolymarketApprovalCalls.build()

        // Assert
        assertThat(calls.none { it.data.contains(CTF_AUTO_REDEEM, ignoreCase = true) }).isTrue()
    }

    @ParameterizedTest
    @MethodSource("provideTestModels")
    fun `GIVEN index WHEN build THEN call data is byte-exact`(model: CallModel) {
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

    internal data class CallModel(val index: Int, val target: String, val spender: String, val data: String)

    /**
     * The set and the order Polymarket's own client writes on chain, read back from 80 consecutive
     * deposit-wallet approval transactions on Polygon (all 80 identical) — an oracle outside our code, per
     * the rule that a known-answer vector must come from the chain, the vendor or the backend.
     */
    private fun provideTestModels() = listOf(
        CallModel(
            index = 0, target = collateral, spender = "conditionalTokens",
            data = "0x095ea7b3" +
                "0000000000000000000000004d97dcd97ec945f40cf65f87097ace5ea0476045" +
                "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
        ),
        CallModel(
            index = 1, target = collateral, spender = "ctfExchange",
            data = "0x095ea7b3" +
                "000000000000000000000000e111180000d2663c0091e4f400237545b87b996b" +
                "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
        ),
        CallModel(
            index = 2, target = conditionalTokens, spender = "ctfExchange",
            data = "0xa22cb465" +
                "000000000000000000000000e111180000d2663c0091e4f400237545b87b996b" +
                "0000000000000000000000000000000000000000000000000000000000000001",
        ),
        CallModel(
            index = 3, target = collateral, spender = "negRiskCtfExchange",
            data = "0x095ea7b3" +
                "000000000000000000000000e2222d279d744050d28e00520010520000310f59" +
                "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
        ),
        CallModel(
            index = 4, target = collateral, spender = "negRiskAdapter",
            data = "0x095ea7b3" +
                "000000000000000000000000d91e80cf2e7be2e162c6513ced06f1dd0da35296" +
                "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
        ),
        CallModel(
            index = 5, target = conditionalTokens, spender = "negRiskCtfExchange",
            data = "0xa22cb465" +
                "000000000000000000000000e2222d279d744050d28e00520010520000310f59" +
                "0000000000000000000000000000000000000000000000000000000000000001",
        ),
        CallModel(
            index = 6, target = conditionalTokens, spender = "negRiskAdapter",
            data = "0xa22cb465" +
                "000000000000000000000000d91e80cf2e7be2e162c6513ced06f1dd0da35296" +
                "0000000000000000000000000000000000000000000000000000000000000001",
        ),
        CallModel(
            index = 7, target = collateral, spender = "ctfCollateralAdapter",
            data = "0x095ea7b3" +
                "000000000000000000000000ada100db00ca00073811820692005400218fce1f" +
                "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
        ),
        CallModel(
            index = 8, target = collateral, spender = "negRiskCtfCollateralAdapter",
            data = "0x095ea7b3" +
                "000000000000000000000000ada2005600dec949baf300f4c6120000bdb6eaab" +
                "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
        ),
        CallModel(
            index = 9, target = conditionalTokens, spender = "ctfCollateralAdapter",
            data = "0xa22cb465" +
                "000000000000000000000000ada100db00ca00073811820692005400218fce1f" +
                "0000000000000000000000000000000000000000000000000000000000000001",
        ),
        CallModel(
            index = 10, target = conditionalTokens, spender = "negRiskCtfCollateralAdapter",
            data = "0xa22cb465" +
                "000000000000000000000000ada2005600dec949baf300f4c6120000bdb6eaab" +
                "0000000000000000000000000000000000000000000000000000000000000001",
        ),
        CallModel(
            index = 11, target = collateral, spender = "exchangeV3",
            data = "0x095ea7b3" +
                "000000000000000000000000e3333700ca9d93003f00f0f71f8515005f6c00aa" +
                "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
        ),
        CallModel(
            index = 12, target = collateral, spender = "routerV3",
            data = "0x095ea7b3" +
                "00000000000000000000000012121212006e4cd160d18e3f00711da5c3372600" +
                "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
        ),
    )

    private companion object {
        const val EXPECTED_CALL_COUNT = 13
        const val EXPECTED_APPROVE_COUNT = 8
        const val EXPECTED_SET_APPROVAL_COUNT = 5
        const val BOTH_ALLOWANCE_KINDS = 2
        const val NEG_RISK_ADAPTER = "d91e80cf2e7be2e162c6513ced06f1dd0da35296"
        const val CTF_AUTO_REDEEM = "f3cfb6a6ebfeb51876289eb235719eb1c65252b0"
    }
}