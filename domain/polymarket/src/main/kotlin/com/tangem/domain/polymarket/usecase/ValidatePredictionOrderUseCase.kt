package com.tangem.domain.polymarket.usecase

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.Raise
import arrow.core.raise.ensure
import com.tangem.domain.polymarket.approval.PolymarketContracts
import com.tangem.domain.polymarket.model.PredictionOrder
import com.tangem.domain.polymarket.model.PredictionOrderDraft
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.domain.polymarket.model.PredictionOrderValidationError
import com.tangem.domain.polymarket.model.PredictionSignedOrder
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

class ValidatePredictionOrderUseCase {

    @Suppress("LongParameterList")
    operator fun invoke(
        signedOrder: PredictionSignedOrder,
        draft: PredictionOrderDraft,
        depositWalletAddress: String,
        marketTokenId: String,
        isNegRisk: Boolean,
        quoteReceivedAtMillis: Long,
        nowMillis: Long,
    ): Either<PredictionOrderValidationError, Unit> = either {
        val order = signedOrder.order

        ensureMarket(draft = draft, marketTokenId = marketTokenId, isNegRisk = isNegRisk)
        ensureLegs(order = order, draft = draft)
        ensurePrice(draft = draft)
        ensureIdentity(order = order, draft = draft, depositWalletAddress = depositWalletAddress)
        ensureTimestamp(order = order, nowMillis = nowMillis)

        val signatureLength = signedOrder.signature.removePrefix(PolymarketContracts.HEX_PREFIX).length
        ensure(signatureLength > PolymarketContracts.SIGNATURE_RSV_HEX_LENGTH) {
            PredictionOrderValidationError.SignatureNotWrapped(length = signatureLength)
        }

        val age = nowMillis - quoteReceivedAtMillis
        ensure(age in 0..MAX_QUOTE_AGE_MILLIS) { PredictionOrderValidationError.StaleQuote(ageMillis = age) }
    }

    private fun ValidationRaise.ensureMarket(draft: PredictionOrderDraft, marketTokenId: String, isNegRisk: Boolean) {
        ensure(draft.tokenId == marketTokenId && draft.isNegRisk == isNegRisk) {
            PredictionOrderValidationError.MarketMismatch(
                expectedTokenId = marketTokenId,
                isNegRiskExpected = isNegRisk,
            )
        }
    }

    private fun ValidationRaise.ensureLegs(order: PredictionOrder, draft: PredictionOrderDraft) {
        ensure(order.makerAmount > BigInteger.ZERO && order.takerAmount > BigInteger.ZERO) {
            PredictionOrderValidationError.NonPositiveLeg(
                makerAmount = order.makerAmount,
                takerAmount = order.takerAmount,
            )
        }
        ensure(order.makerAmount == draft.makerAmount && order.takerAmount == draft.takerAmount) {
            PredictionOrderValidationError.LegsNotFromQuote(
                signedMakerAmount = order.makerAmount,
                signedTakerAmount = order.takerAmount,
            )
        }
        ensure(order.tokenId == draft.tokenId.toBigIntegerOrNull()) {
            PredictionOrderValidationError.MarketMismatch(
                expectedTokenId = draft.tokenId,
                isNegRiskExpected = draft.isNegRisk,
            )
        }
    }

    private fun ValidationRaise.ensurePrice(draft: PredictionOrderDraft) {
        ensure(draft.worstCasePrice > BigDecimal.ZERO && draft.worstCasePrice < BigDecimal.ONE) {
            PredictionOrderValidationError.PriceOutOfRange(worstCasePrice = draft.worstCasePrice)
        }

        val (collateral, shares) = when (draft.side) {
            PredictionOrderSide.BUY -> draft.makerAmount to draft.takerAmount
            PredictionOrderSide.SELL -> draft.takerAmount to draft.makerAmount
        }
        val toleratedCollateral = BigDecimal(shares).multiply(draft.worstCasePrice)
        val signedCollateral = BigDecimal(collateral)
        val slack = quantizationSlack(draft = draft)
        val isWithinTolerance = when (draft.side) {
            PredictionOrderSide.BUY -> signedCollateral <= toleratedCollateral + slack
            PredictionOrderSide.SELL -> signedCollateral >= toleratedCollateral - slack
        }

        ensure(isWithinTolerance) {
            PredictionOrderValidationError.PriceWorseThanTolerated(
                implied = signedCollateral.divide(BigDecimal(shares), PRICE_SCALE, RoundingMode.HALF_UP),
                tolerated = draft.worstCasePrice,
            )
        }
    }

    private fun quantizationSlack(draft: PredictionOrderDraft): BigDecimal {
        val quantum = BigDecimal(draft.takerAmountQuantum)
        val quantumInCollateral = when (draft.side) {
            PredictionOrderSide.BUY -> quantum.multiply(draft.worstCasePrice)
            PredictionOrderSide.SELL -> quantum
        }

        return quantumInCollateral.setScale(0, RoundingMode.CEILING)
    }

    private fun ValidationRaise.ensureIdentity(
        order: PredictionOrder,
        draft: PredictionOrderDraft,
        depositWalletAddress: String,
    ) {
        ensure(order.signatureType == PolymarketContracts.SIGNATURE_TYPE_DEPOSIT_WALLET) {
            PredictionOrderValidationError.WrongSignatureType(signatureType = order.signatureType)
        }
        ensure(order.maker.equals(depositWalletAddress, ignoreCase = true)) {
            PredictionOrderValidationError.MakerIsNotDepositWallet(
                maker = order.maker,
                expected = depositWalletAddress,
            )
        }
        ensure(order.signer.equals(order.maker, ignoreCase = true)) {
            PredictionOrderValidationError.SignerIsNotMaker(signer = order.signer, maker = order.maker)
        }
        ensure(order.side == draft.side) {
            PredictionOrderValidationError.SideMismatch(signed = order.side, quoted = draft.side)
        }
        ensure(order.builder.isNotBlank()) {
            PredictionOrderValidationError.MissingBuilderCode(builder = order.builder)
        }
        ensure(order.builder == draft.builderCode) {
            PredictionOrderValidationError.BuilderCodeMismatch(signed = order.builder, quoted = draft.builderCode)
        }
    }

    private fun ValidationRaise.ensureTimestamp(order: PredictionOrder, nowMillis: Long) {
        ensure(order.timestampMillis > MILLIS_THRESHOLD) {
            PredictionOrderValidationError.TimestampNotInMillis(timestampMillis = order.timestampMillis)
        }
        ensure(order.timestampMillis <= nowMillis) {
            PredictionOrderValidationError.TimestampInFuture(
                timestampMillis = order.timestampMillis,
                nowMillis = nowMillis,
            )
        }
    }

    private companion object {
        const val MILLIS_THRESHOLD = 1_000_000_000_000L

        const val MAX_QUOTE_AGE_MILLIS = 60_000L

        const val PRICE_SCALE = 8
    }
}

private typealias ValidationRaise = Raise<PredictionOrderValidationError>