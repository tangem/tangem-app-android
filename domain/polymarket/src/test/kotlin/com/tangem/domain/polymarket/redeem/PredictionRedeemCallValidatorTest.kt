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
        fun `GIVEN one adapter redeemPositions call for both index sets WHEN validated THEN it is accepted`() {
            // Arrange
            val calls = served(PolymarketMarketVersion.V1, v1Call())

            // Act
            val actual = PredictionRedeemCallValidator.validate(calls, expectation)

            // Assert
            assertThat(actual).isEqualTo(Unit.right())
        }

        @Test
        fun `GIVEN a redeemPositions call for one index set WHEN validated THEN it is accepted`() {
            // Arrange
            val calls = served(PolymarketMarketVersion.V1, v1Call(indexSets = listOf(2)))

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
        fun `GIVEN upper-case calldata and a hex zero value WHEN validated THEN it is accepted`() {
            // Arrange
            val call = v1Call(value = "0x0").let { it.copy(data = "0x" + it.data.drop(2).uppercase()) }
            val calls = served(PolymarketMarketVersion.V1, call)

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
                calls = served(PolymarketMarketVersion.V1, v1Call(selector = MERGE_POSITIONS_SELECTOR)),
                rejection = Rejection.Calldata(0),
            ),
            RejectionModel(
                calls = served(PolymarketMarketVersion.V1, v1Call(conditionId = OTHER_CONDITION_ID)),
                rejection = Rejection.Calldata(0),
            ),
            RejectionModel(
                calls = served(
                    PolymarketMarketVersion.V1,
                    v1Call(parentCollectionId = CONDITION_ID, conditionId = OTHER_CONDITION_ID),
                ),
                rejection = Rejection.Calldata(0),
            ),
            RejectionModel(
                calls = served(PolymarketMarketVersion.V1, v1Call(parentCollectionId = OTHER_CONDITION_ID)),
                rejection = Rejection.Calldata(0),
            ),
            RejectionModel(
                calls = served(PolymarketMarketVersion.V1, v1Call(indexSetsOffset = "a0")),
                rejection = Rejection.Calldata(0),
            ),
            RejectionModel(
                calls = served(PolymarketMarketVersion.V1, v1Call(indexSets = emptyList())),
                rejection = Rejection.Calldata(0),
            ),
            RejectionModel(
                calls = served(PolymarketMarketVersion.V1, v1Call(indexSets = listOf(1, 2, 4))),
                rejection = Rejection.Calldata(0),
            ),
            RejectionModel(
                calls = served(PolymarketMarketVersion.V1, v1Call(trailingHex = word(CONDITION_ID))),
                rejection = Rejection.Calldata(0),
            ),
            RejectionModel(
                calls = served(PolymarketMarketVersion.V1, v1Call(trailingHex = "00")),
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
                calls = served(PolymarketMarketVersion.V2, v2Call(0, selector = TRANSFER_SELECTOR)),
                rejection = Rejection.Calldata(0),
            ),
            RejectionModel(
                calls = served(PolymarketMarketVersion.V2, v2Call(0, positionWord = LEGACY_POSITION_WORD)),
                rejection = Rejection.Calldata(0),
            ),
            RejectionModel(
                calls = served(PolymarketMarketVersion.V2, v2Call(0, positionWord = ASSET_ID_WORD)),
                rejection = Rejection.Calldata(0),
            ),
            RejectionModel(
                calls = served(PolymarketMarketVersion.V2, v2Call(outcome = 2)),
                rejection = Rejection.Calldata(0),
            ),
            RejectionModel(
                calls = served(PolymarketMarketVersion.V2, v2Call(0, amountHex = "0")),
                rejection = Rejection.Calldata(0),
            ),
            RejectionModel(
                calls = served(PolymarketMarketVersion.V2, v2Call(0, trailingHex = word("0"))),
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

        /** A decimal token id and, worked out apart from the validator, its 32-byte hex and the `bytes31` word. */
        const val ASSET_ID = "21742633143463906290569050155826241533067272736897614950488156847949938836455"
        const val ASSET_ID_WORD = "3011e4ede0f6befa0ad3f571001d3e1ffeef3d4af78c3112aaac90416e3a43e7"
        const val ASSET_POSITION_WORD = "3011e4ede0f6befa0ad3f571001d3e1ffeef3d4af78c3112aaac90416e3a4300"
        const val LEGACY_POSITION_WORD = "da5b40ea9292a66146ad7a808a1a6dc26051afff6818ca5807e4a36e8d234500"

        const val REDEEM_POSITIONS_SELECTOR = "01b7037c"
        const val MERGE_POSITIONS_SELECTOR = "9e7212ad"
        const val REDEEM_SELECTOR = "d217a3cc"
        const val TRANSFER_SELECTOR = "a9059cbb"

        fun served(version: PolymarketMarketVersion?, first: PolymarketApprovalCall, vararg rest: PolymarketApprovalCall) =
            PredictionRedeemCalls(version = version, calls = nonEmptyListOf(first, *rest))

        fun word(hex: String) = hex.removePrefix("0x").lowercase().padStart(64, '0')

        /** `redeemPositions(address,bytes32,bytes32,uint256[])` as the adapters' ABI lays it out */
        fun v1Call(
            target: String = PolymarketContracts.CTF_COLLATERAL_ADAPTER,
            selector: String = REDEEM_POSITIONS_SELECTOR,
            parentCollectionId: String = "0",
            conditionId: String = CONDITION_ID,
            indexSetsOffset: String = "80",
            indexSets: List<Int> = listOf(1, 2),
            trailingHex: String = "",
            value: String = "0",
        ) = PolymarketApprovalCall(
            target = target,
            value = value,
            data = "0x" + selector +
                word(PolymarketContracts.COLLATERAL) +
                word(parentCollectionId) +
                word(conditionId) +
                word(indexSetsOffset) +
                word(indexSets.size.toString(16)) +
                indexSets.joinToString(separator = "") { word(it.toString(16)) } +
                trailingHex,
        )

        /** `redeem(bytes31,uint256,uint256)` on the Router */
        fun v2Call(
            outcome: Int,
            target: String = PolymarketContracts.ROUTER_V3,
            selector: String = REDEEM_SELECTOR,
            positionWord: String = ASSET_POSITION_WORD,
            amountHex: String = "4c4b40",
            trailingHex: String = "",
            value: String = "0",
        ) = PolymarketApprovalCall(
            target = target,
            value = value,
            data = "0x" + selector + positionWord + word(outcome.toString(16)) + word(amountHex) + trailingHex,
        )
    }
}