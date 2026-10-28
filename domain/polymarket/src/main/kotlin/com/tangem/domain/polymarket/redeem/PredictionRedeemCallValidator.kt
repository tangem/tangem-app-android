package com.tangem.domain.polymarket.redeem

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.domain.polymarket.approval.PolymarketContracts
import com.tangem.domain.polymarket.model.PolymarketApprovalCall
import com.tangem.domain.polymarket.model.PolymarketMarketVersion
import com.tangem.domain.polymarket.model.PredictionRedeemCalls
import java.math.BigInteger

/**
 * Checks the redeem calls the backend served against the position they are meant to redeem, before the card
 * signs them. Addresses and calldata are compared ignoring case: the backend lower-cases what it serves.
 *
 * A V1 market redeems with one call on its collateral adapter, `redeemPositions(address collateral, bytes32
 * parentCollectionId, bytes32 conditionId, uint256[] indexSets)` — the only redeem function either adapter
 * exposes: the parent collection is the root one, the condition is the position's market, the index sets are at
 * most one per outcome and nothing follows them. A V2 market redeems with one call per held outcome on the
 * Router, `redeem(bytes31 positionId, uint256 outcome, uint256 amount)`: the position id is the asset id with its
 * last byte dropped, outcomes are 0 or 1 and differ between the calls, and the amount is positive. A position
 * still held in a V2 market's legacy token fails the position id check by design.
 */
object PredictionRedeemCallValidator {

    private const val HEX_RADIX = 16
    private const val WORD_HEX_LENGTH = 64
    private const val SELECTOR_HEX_LENGTH = 8
    private const val BYTE_HEX_LENGTH = 2
    private val ZERO_WORD = "".padStart(WORD_HEX_LENGTH, '0')

    private const val V1_REDEEM_SELECTOR = "01b7037c"
    private const val V1_PARENT_COLLECTION_WORD = 1
    private const val V1_CONDITION_WORD = 2
    private const val V1_INDEX_SETS_OFFSET_WORD = 3
    private const val V1_INDEX_SETS_LENGTH_WORD = 4
    private const val V1_HEAD_WORDS = 4
    private const val V1_MAX_INDEX_SETS = 2L
    private val V1_INDEX_SETS_OFFSET = (V1_HEAD_WORDS * WORD_HEX_LENGTH / BYTE_HEX_LENGTH)
        .toString(HEX_RADIX)
        .padStart(WORD_HEX_LENGTH, '0')
    private val V1_INDEX_SETS_RANGE = BigInteger.ONE..BigInteger.valueOf(V1_MAX_INDEX_SETS)

    private const val V2_REDEEM_SELECTOR = "d217a3cc"
    private const val V2_REDEEM_WORDS = 3
    private const val V2_MAX_CALLS = 2
    private val OUTCOME_RANGE = BigInteger.ZERO..BigInteger.ONE

    sealed interface Rejection {

        data class VersionMismatch(
            val served: PolymarketMarketVersion,
            val expected: PolymarketMarketVersion,
        ) : Rejection

        data class CallCount(val count: Int) : Rejection

        data class Value(val index: Int) : Rejection

        data class Target(val index: Int) : Rejection

        data class Calldata(val index: Int) : Rejection

        data object RepeatedOutcome : Rejection

        /** The position's market version is one this build cannot redeem on. */
        data object UnsupportedVersion : Rejection
    }

    /**
     * @property conditionId the market the position belongs to
     * @property assetId the position's token, which a V2 redeem must name
     */
    data class Expectation(
        val version: PolymarketMarketVersion,
        val conditionId: String,
        val assetId: String,
        val isNegRisk: Boolean,
    )

    fun validate(calls: PredictionRedeemCalls, expectation: Expectation): Either<Rejection, Unit> = either {
        ensure(expectation.version != PolymarketMarketVersion.UNKNOWN) { Rejection.UnsupportedVersion }
        val served = calls.version
        ensure(served == null || served == expectation.version) {
            Rejection.VersionMismatch(served = requireNotNull(served), expected = expectation.version)
        }
        calls.calls.forEachIndexed { index, call ->
            ensure(call.value.isZero()) { Rejection.Value(index) }
        }
        when (expectation.version) {
            PolymarketMarketVersion.V1 -> validateV1(calls.calls, expectation).bind()
            PolymarketMarketVersion.V2 -> validateV2(calls.calls, expectation).bind()
            PolymarketMarketVersion.UNKNOWN -> raise(Rejection.UnsupportedVersion)
        }
    }

    private fun validateV1(calls: List<PolymarketApprovalCall>, expectation: Expectation): Either<Rejection, Unit> =
        either {
            ensure(calls.size == 1) { Rejection.CallCount(calls.size) }
            val call = calls.single()
            val adapter = if (expectation.isNegRisk) {
                PolymarketContracts.NEG_RISK_CTF_COLLATERAL_ADAPTER
            } else {
                PolymarketContracts.CTF_COLLATERAL_ADAPTER
            }
            ensure(call.target.equals(adapter, ignoreCase = true)) { Rejection.Target(index = 0) }
            ensure(call.data.redeemsCondition(expectation.conditionId)) { Rejection.Calldata(index = 0) }
        }

    /**
     * Whether the calldata is `redeemPositions` on the root collection of [conditionId] with one or two index sets
     * and nothing after them. The collateral address is the adapter's to accept.
     */
    private fun String.redeemsCondition(conditionId: String): Boolean {
        val condition = conditionId.removePrefix(PolymarketContracts.HEX_PREFIX).lowercase()
        if (condition.length != WORD_HEX_LENGTH) return false
        val words = argumentWords(V1_REDEEM_SELECTOR) ?: return false
        if (words.size <= V1_INDEX_SETS_LENGTH_WORD) return false
        val indexSets = words[V1_INDEX_SETS_LENGTH_WORD].toBigIntegerOrNull(HEX_RADIX) ?: return false
        return words[V1_PARENT_COLLECTION_WORD] == ZERO_WORD &&
            words[V1_CONDITION_WORD] == condition &&
            words[V1_INDEX_SETS_OFFSET_WORD] == V1_INDEX_SETS_OFFSET &&
            indexSets in V1_INDEX_SETS_RANGE &&
            words.size == V1_INDEX_SETS_LENGTH_WORD + 1 + indexSets.toInt()
    }

    /** `0`, `00` and `0x0` are all the zero the relayer accepts; anything else moves native value. */
    private fun String.isZero(): Boolean {
        val digits = removePrefix(PolymarketContracts.HEX_PREFIX)
        return digits.isNotEmpty() && digits.all { it == '0' }
    }

    private fun validateV2(calls: List<PolymarketApprovalCall>, expectation: Expectation): Either<Rejection, Unit> =
        either {
            ensure(calls.size in 1..V2_MAX_CALLS) { Rejection.CallCount(calls.size) }
            val positionWord = expectation.assetId.toPositionWord()
            val outcomes = calls.mapIndexed { index, call ->
                ensure(call.target.equals(PolymarketContracts.ROUTER_V3, ignoreCase = true)) { Rejection.Target(index) }
                val words = call.data.argumentWords(V2_REDEEM_SELECTOR)?.takeIf { it.size == V2_REDEEM_WORDS }
                ensure(words != null && positionWord != null && words[0] == positionWord) { Rejection.Calldata(index) }
                val outcome = words[1].toBigIntegerOrNull(HEX_RADIX)
                val amount = words[2].toBigIntegerOrNull(HEX_RADIX)
                ensure(outcome != null && outcome in OUTCOME_RANGE) { Rejection.Calldata(index) }
                ensure(amount != null && amount.signum() > 0) { Rejection.Calldata(index) }
                outcome
            }
            ensure(outcomes.distinct().size == outcomes.size) { Rejection.RepeatedOutcome }
        }

    /** The asset id as a 32-byte word with its last byte zeroed — how `bytes31` lands in a calldata word. */
    private fun String.toPositionWord(): String? {
        val id = toBigIntegerOrNull() ?: return null
        if (id.signum() < 0) return null
        val hex = id.toString(HEX_RADIX)
        if (hex.length > WORD_HEX_LENGTH) return null
        return hex.padStart(WORD_HEX_LENGTH, '0').dropLast(BYTE_HEX_LENGTH) + "00"
    }

    /** @return the whole 32-byte words after [selector], lower-cased, or null for another selector or a torn word */
    private fun String.argumentWords(selector: String): List<String>? {
        val body = removePrefix(PolymarketContracts.HEX_PREFIX).lowercase()
        if (!body.startsWith(selector)) return null
        val arguments = body.drop(SELECTOR_HEX_LENGTH)
        if (arguments.length % WORD_HEX_LENGTH != 0) return null
        return arguments.chunked(WORD_HEX_LENGTH)
    }
}