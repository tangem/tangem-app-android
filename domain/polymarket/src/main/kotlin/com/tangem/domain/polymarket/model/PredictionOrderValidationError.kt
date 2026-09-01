package com.tangem.domain.polymarket.model

import java.math.BigDecimal
import java.math.BigInteger

sealed interface PredictionOrderValidationError : PolymarketError {

    data class NonPositiveLeg(val makerAmount: BigInteger, val takerAmount: BigInteger) :
        PredictionOrderValidationError

    data class LegsNotFromQuote(
        val signedMakerAmount: BigInteger,
        val signedTakerAmount: BigInteger,
    ) : PredictionOrderValidationError

    data class PriceOutOfRange(val worstCasePrice: BigDecimal) : PredictionOrderValidationError

    data class PriceWorseThanTolerated(val implied: BigDecimal, val tolerated: BigDecimal) :
        PredictionOrderValidationError

    data class TimestampNotInMillis(val timestampMillis: Long) : PredictionOrderValidationError

    data class TimestampInFuture(val timestampMillis: Long, val nowMillis: Long) : PredictionOrderValidationError

    data class WrongSignatureType(val signatureType: Int) : PredictionOrderValidationError

    data class MakerIsNotDepositWallet(val maker: String, val expected: String) : PredictionOrderValidationError

    data class MarketMismatch(val expectedTokenId: String, val isNegRiskExpected: Boolean) :
        PredictionOrderValidationError

    data class SignerIsNotMaker(val signer: String, val maker: String) : PredictionOrderValidationError

    data class SideMismatch(val signed: PredictionOrderSide, val quoted: PredictionOrderSide) :
        PredictionOrderValidationError

    data class MissingBuilderCode(val builder: String) : PredictionOrderValidationError

    data class BuilderCodeMismatch(val signed: String, val quoted: String) : PredictionOrderValidationError

    data class SignatureNotWrapped(val length: Int) : PredictionOrderValidationError

    data class StaleQuote(val ageMillis: Long) : PredictionOrderValidationError
}