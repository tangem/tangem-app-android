package com.tangem.domain.polymarket.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
import com.tangem.domain.polymarket.model.PredictionOrderDraft
import com.tangem.domain.polymarket.model.PredictionOrderDraftError
import com.tangem.domain.polymarket.model.PredictionOrderQuote
import com.tangem.domain.polymarket.model.PredictionOrderSide
import java.math.BigDecimal
import java.math.BigInteger

class BuildPredictionOrderDraftUseCase {

    /**
     * Which quoted leg is maker and which is taker flips by side, because the leg the user *types* is
     * always the leg they give up.
     *
     * @param quote the only source of the legs — the typed amount is deliberately not a parameter, since
     *  the backend normalises it onto the market's grid and only the returned figure may be signed
     */
    operator fun invoke(
        quote: PredictionOrderQuote,
        assetId: String,
        isNegRisk: Boolean,
        side: PredictionOrderSide,
    ): Either<PredictionOrderDraftError, PredictionOrderDraft> = either {
        ensure(quote.status.isPlaceable) { PredictionOrderDraftError.NotPlaceable(status = quote.status) }
        ensure(quote.side == side) {
            PredictionOrderDraftError.SideMismatch(quoted = quote.side, requested = side)
        }

        val collateral = quote.notional.toOnChainUnit().bind()
        val shares = quote.shares.toOnChainUnit().bind()

        PredictionOrderDraft(
            tokenId = assetId,
            isNegRisk = isNegRisk,
            side = side,
            makerAmount = if (side == PredictionOrderSide.BUY) collateral else shares,
            takerAmount = if (side == PredictionOrderSide.BUY) shares else collateral,
            worstCasePrice = quote.worstCasePrice,
            builderCode = quote.builderCode,
        )
    }

    private fun BigDecimal.toOnChainUnit(): Either<PredictionOrderDraftError, BigInteger> {
        val trimmed = stripTrailingZeros()

        return if (trimmed.scale() > ON_CHAIN_DECIMALS) {
            PredictionOrderDraftError.AmountNotRepresentable(value = this).left()
        } else {
            trimmed.movePointRight(ON_CHAIN_DECIMALS).toBigIntegerExact().right()
        }
    }

    private companion object {
        const val ON_CHAIN_DECIMALS = 6
    }
}