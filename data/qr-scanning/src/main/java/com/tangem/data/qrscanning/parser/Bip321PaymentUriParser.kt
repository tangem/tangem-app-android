package com.tangem.data.qrscanning.parser

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.qrscanning.models.ClassifiedQrContent

internal class Bip321PaymentUriParser(
    private val blockchainDataProvider: QrContentClassifierParser.BlockchainDataProvider,
) : PaymentUriParser {

    override fun parse(
        qrCode: String,
        coins: List<CryptoCurrency.Coin>,
        allCurrencies: List<CryptoCurrency>,
    ): PaymentUriParser.ParseResult {
        val schemeAndRest = extractSchemeAndRest(qrCode, coins)
            ?: return PaymentUriParser.ParseResult.NotRecognized
        val (matchingCoins, withoutScheme) = schemeAndRest

        val parsed = QrSentUriParser().parse(withoutScheme)
            ?: return PaymentUriParser.ParseResult.RecognizedError(
                ClassifiedQrContent.Error.Unrecognized(qrCode),
            )

        val matchingNetworkIds = matchingCoins.map { it.network.id }.toSet()
        val matchingCurrencies = allCurrencies.filter { it.network.id in matchingNetworkIds }
        if (matchingCurrencies.isEmpty()) {
            return PaymentUriParser.ParseResult.RecognizedError(
                ClassifiedQrContent.Error.UnsupportedNetwork,
            )
        }

        return PaymentUriParser.ParseResult.Success(
            ClassifiedQrContent.PaymentUri(
                address = parsed.address,
                amount = parsed.amount,
                memo = parsed.memo,
                matchingCurrencies = matchingCurrencies,
            ),
        )
    }

    private fun extractSchemeAndRest(
        qrCode: String,
        coins: List<CryptoCurrency.Coin>,
    ): Pair<List<CryptoCurrency.Coin>, String>? {
        for (coin in coins) {
            val schemes = blockchainDataProvider.getShareSchemes(coin.network)
            for (scheme in schemes) {
                if (qrCode.startsWith(scheme, ignoreCase = true)) {
                    val withoutScheme = qrCode.removeRange(0, scheme.length)
                    val allMatchingCoins = coins.filter { c ->
                        blockchainDataProvider.getShareSchemes(c.network).any { it.equals(scheme, ignoreCase = true) }
                    }
                    return allMatchingCoins to withoutScheme
                }
            }
        }
        return null
    }
}