package com.tangem.data.qrscanning.parser

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.qrscanning.models.ClassifiedQrContent

internal interface PaymentUriParser {

    fun parse(qrCode: String, coins: List<CryptoCurrency.Coin>, allCurrencies: List<CryptoCurrency>): ParseResult

    sealed class ParseResult {
        /** URI format not recognized by this parser. */
        data object NotRecognized : ParseResult()

        /** URI format recognized but resulted in an error. */
        data class RecognizedError(val error: ClassifiedQrContent.Error) : ParseResult()

        /** Successfully parsed with matching currencies. */
        data class Success(val content: ClassifiedQrContent.PaymentUri) : ParseResult()

        /** Successfully parsed but QR contains unsupported parameters. */
        data class SuccessWithWarning(
            val content: ClassifiedQrContent.PaymentUri,
            val unsupportedParams: Map<String, String>,
        ) : ParseResult()
    }
}