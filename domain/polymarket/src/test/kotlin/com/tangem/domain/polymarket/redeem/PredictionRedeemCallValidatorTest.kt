package com.tangem.domain.polymarket.redeem

import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.approval.PolymarketContracts
import com.tangem.domain.polymarket.model.PolymarketApprovalCall
import com.tangem.domain.polymarket.model.PolymarketMarketVersion
import com.tangem.domain.polymarket.model.PredictionRedeemCalls
import com.tangem.domain.polymarket.redeem.PredictionRedeemCallValidator.Expectation
import com.tangem.domain.polymarket.redeem.PredictionRedeemCallValidator.Rejection
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigInteger

internal class PredictionRedeemCallValidatorTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class V1 {

        private val expectation = Expectation(
            version = PolymarketMarketVersion.V1,
            conditionId = CONDITION_ID,
            assetId = ASSET_ID,
            isNegRisk = false,
        )

        @Test
        fun `GIVEN one adapter call naming the condition WHEN validated THEN it is accepted`() {
            // Arrange
            val calls = served(PolymarketMarketVersion.V1, v1Call())

            // Act
            val actual = PredictionRedeemCallValidator.validate(calls, expectation)

            // Assert
            assertThat(actual).isEqualTo(Unit.right())
        }

        @Test
        fun `GIVEN a neg risk market WHEN the call targets the neg risk adapter in another case THEN it is accepted`() {
            // Arrange
            val call = v1Call(target = PolymarketContracts.NEG_RISK_CTF_COLLATERAL_ADAPTER.lowercase())
            val calls = served(PolymarketMarketVersion.V1, call)

            // Act
            val actual = PredictionRedeemCallValidator.validate(calls, expectation.copy(isNegRisk = true))

            // Assert
            assertThat(actual).isEqualTo(Unit.right())
        }

        @Test
        fun `GIVEN a hex zero value WHEN validated THEN it is accepted`() {
            // Arrange
            val calls = served(PolymarketMarketVersion.V1, v1Call(value = "0x0"))

            // Act
            val actual = PredictionRedeemCallValidator.validate(calls, expectation)

            // Assert
            assertThat(actual).isEqualTo(Unit.right())
        }

        @Test
        fun `GIVEN no served version WHEN validated THEN the position's version decides`() {
            // Arrange
            val calls = served(version = null, v1Call())

            // Act
            val actual = PredictionRedeemCallValidator.validate(calls, expectation)

            // Assert
            assertThat(actual).isEqualTo(Unit.right())
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a wrong V1 answer WHEN validated THEN it is refused`(model: RejectionModel) {
            // Act
            val actual = PredictionRedeemCallValidator.validate(model.calls, expectation)

            // Assert
            assertThat(actual).isEqualTo(model.rejection.left())
        }

        private fun provideTestModels() = listOf(
            RejectionModel(
                calls = served(PolymarketMarketVersion.V2, v1Call()),
                rejection = Rejection.VersionMismatch(PolymarketMarketVersion.V2, PolymarketMarketVersion.V1),
            ),
            RejectionModel(
                calls = served(PolymarketMarketVersion.V1, v1Call(), v1Call()),
                rejection = Rejection.CallCount(2),
            ),
            RejectionModel(
                calls = served(PolymarketMarketVersion.V1, v1Call(target = PolymarketContracts.NEG_RISK_CTF_COLLATERAL_ADAPTER)),
                rejection = Rejection.Target(0),
            ),
            RejectionModel(
                calls = served(PolymarketMarketVersion.V1, v1Call(conditionId = OTHER_CONDITION_ID)),
                rejection = Rejection.Calldata(0),
            ),
            RejectionModel(
                calls = served(PolymarketMarketVersion.V1, v1Call(leadingWordHex = "aa")),
                rejection = Rejection.Calldata(0),
            ),
            RejectionModel(
                calls = served(PolymarketMarketVersion.V1, v1Call(value = "1")),
                rejection = Rejection.Value(0),
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class V2 {

        private val expectation = Expectation(
            version = PolymarketMarketVersion.V2,
            conditionId = CONDITION_ID,
            assetId = ASSET_ID,
            isNegRisk = false,
        )

        @Test
        fun `GIVEN two router calls for both outcomes WHEN validated THEN they are accepted`() {
            // Arrange
            val calls = served(PolymarketMarketVersion.V2, v2Call(outcome = 0), v2Call(outcome = 1))

            // Act
            val actual = PredictionRedeemCallValidator.validate(calls, expectation)

            // Assert
            assertThat(actual).isEqualTo(Unit.right())
        }

        @Test
        fun `GIVEN one router call with upper-case calldata WHEN validated THEN it is accepted`() {
            // Arrange
            val call = v2Call(outcome = 1).let {
                it.copy(target = it.target.uppercase(), data = "0x" + it.data.drop(2).uppercase())
            }

            // Act
            val actual = PredictionRedeemCallValidator.validate(served(PolymarketMarketVersion.V2, call), expectation)

            // Assert
            assertThat(actual).isEqualTo(Unit.right())
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a wrong V2 answer WHEN validated THEN it is refused`(model: RejectionModel) {
            // Act
            val actual = PredictionRedeemCallValidator.validate(model.calls, expectation)

            // Assert
            assertThat(actual).isEqualTo(model.rejection.left())
        }

        private fun provideTestModels() = listOf(
            RejectionModel(
                calls = served(PolymarketMarketVersion.V1, v2Call(outcome = 0)),
                rejection = Rejection.VersionMismatch(PolymarketMarketVersion.V1, PolymarketMarketVersion.V2),
            ),
            RejectionModel(
                calls = served(PolymarketMarketVersion.V2, v2Call(0), v2Call(1), v2Call(0)),
                rejection = Rejection.CallCount(3),
            ),
            RejectionModel(
                calls = served(PolymarketMarketVersion.V2, v2Call(0), v2Call(1, target = PolymarketContracts.EXCHANGE_V3)),
                rejection = Rejection.Target(1),
            ),
            RejectionModel(
                calls = served(PolymarketMarketVersion.V2, v2Call(0, selector = "a9059cbb")),
                rejection = Rejection.Calldata(0),
            ),
            RejectionModel(
                calls = served(PolymarketMarketVersion.V2, v2Call(0, assetId = LEGACY_ASSET_ID)),
                rejection = Rejection.Calldata(0),
            ),
            RejectionModel(
                calls = served(PolymarketMarketVersion.V2, v2Call(outcome = 2)),
                rejection = Rejection.Calldata(0),
            ),
            RejectionModel(
                calls = served(PolymarketMarketVersion.V2, v2Call(0, amount = BigInteger.ZERO)),
                rejection = Rejection.Calldata(0),
            ),
            RejectionModel(
                calls = served(PolymarketMarketVersion.V2, v2Call(1), v2Call(1)),
                rejection = Rejection.RepeatedOutcome,
            ),
            RejectionModel(
                calls = served(PolymarketMarketVersion.V2, v2Call(0, value = "5")),
                rejection = Rejection.Value(0),
            ),
        )
    }

    @Test
    fun `GIVEN an unknown position version WHEN validated THEN nothing is accepted`() {
        // Arrange
        val expectation = Expectation(
            version = PolymarketMarketVersion.UNKNOWN,
            conditionId = CONDITION_ID,
            assetId = ASSET_ID,
            isNegRisk = false,
        )

        // Act
        val actual = PredictionRedeemCallValidator.validate(served(version = null, v1Call()), expectation)

        // Assert
        assertThat(actual).isEqualTo(Rejection.UnsupportedVersion.left())
    }

    internal data class RejectionModel(val calls: PredictionRedeemCalls, val rejection: Rejection)

    private companion object {
        const val CONDITION_ID = "0x94947A23d1f1f6E0dFb6f2B2C4d1A4e9c0D1e2F3a4b5c6d7e8f9a0b1c2d3e4f5"
        const val OTHER_CONDITION_ID = "0x0000000000000000000000000000000000000000000000000000000000000001"
        const val ASSET_ID = "21742633143463906290569050155826241533067272736897614950488156847949938836455"
        const val LEGACY_ASSET_ID = "98765432109876543210987654321098765432109876543210987654321098765432109876543"
        const val V2_SELECTOR = "d217a3cc"

        fun served(version: PolymarketMarketVersion?, first: PolymarketApprovalCall, vararg rest: PolymarketApprovalCall) =
            PredictionRedeemCalls(version = version, calls = nonEmptyListOf(first, *rest))

        /** @param leadingWordHex extra hex before the words, which shifts the condition off its word boundary */
        fun v1Call(
            target: String = PolymarketContracts.CTF_COLLATERAL_ADAPTER,
            conditionId: String = CONDITION_ID,
            value: String = "0",
            leadingWordHex: String = "",
        ) = PolymarketApprovalCall(
            target = target,
            value = value,
            data = "0x01b7037c" + leadingWordHex +
                PolymarketContracts.COLLATERAL.removePrefix("0x").lowercase().padStart(64, '0') +
                "".padStart(64, '0') +
                conditionId.removePrefix("0x").lowercase() +
                "".padStart(64, '0'),
        )

        fun v2Call(
            outcome: Int,
            target: String = PolymarketContracts.ROUTER_V3,
            selector: String = V2_SELECTOR,
            assetId: String = ASSET_ID,
            amount: BigInteger = BigInteger("5000000"),
            value: String = "0",
        ): PolymarketApprovalCall {
            val positionWord = BigInteger(assetId).toString(16).padStart(64, '0').dropLast(2) + "00"
            return PolymarketApprovalCall(
                target = target,
                value = value,
                data = "0x" + selector +
                    positionWord +
                    outcome.toString(16).padStart(64, '0') +
                    amount.toString(16).padStart(64, '0'),
            )
        }
    }
}