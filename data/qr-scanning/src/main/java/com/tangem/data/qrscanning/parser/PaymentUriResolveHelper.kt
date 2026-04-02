package com.tangem.data.qrscanning.parser

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.qrscanning.models.ClassifiedQrContent
import java.math.BigDecimal

internal class PaymentUriResolveHelper {

    fun parseUri(qrCode: String, scheme: String): QrSentUriParser.Result? {
        val withoutScheme = qrCode.removeRange(0, scheme.length)
        return QrSentUriParser().parse(withoutScheme)
    }

    fun resolveTokenTransfer(
        context: ResolveContext,
        contractAddress: String,
        interpretAmount: (BigDecimal, Int) -> BigDecimal = { raw, _ -> raw },
    ): PaymentUriParser.ParseResult {
        val matchingTokens = context.allCurrencies.filterIsInstance<CryptoCurrency.Token>()
            .filter { token ->
                token.network.id in context.matchingNetworkIds &&
                    token.contractAddress.equals(contractAddress, ignoreCase = true)
            }

        if (matchingTokens.isEmpty()) {
            return PaymentUriParser.ParseResult.RecognizedError(
                ClassifiedQrContent.Error.UnsupportedNetwork(
                    raw = context.qrCode,
                    blockchain = context.blockchainName,
                ),
            )
        }

        val amount = context.parsed.amount?.let {
            interpretAmount(it, matchingTokens.first().decimals)
        }

        return PaymentUriParser.ParseResult.Success(
            ClassifiedQrContent.PaymentUri(
                address = context.parsed.address,
                amount = amount,
                memo = context.parsed.memo?.second,
                matchingCurrencies = matchingTokens,
            ),
        )
    }

    fun resolveNativeOrAll(
        context: ResolveContext,
        interpretAmount: (BigDecimal, Int) -> BigDecimal = { raw, _ -> raw },
    ): PaymentUriParser.ParseResult {
        val decimals = context.matchingCoins.firstOrNull()?.decimals
        val amount = if (decimals != null) {
            context.parsed.amount?.let { interpretAmount(it, decimals) }
        } else {
            context.parsed.amount
        }

        val matchingCurrencies = if (context.parsed.amount != null) {
            context.matchingCoins
        } else {
            context.allCurrencies.filter { it.network.id in context.matchingNetworkIds }
        }

        return PaymentUriParser.ParseResult.Success(
            ClassifiedQrContent.PaymentUri(
                address = context.parsed.address,
                amount = amount,
                memo = context.parsed.memo?.second,
                matchingCurrencies = matchingCurrencies,
            ),
        )
    }

    fun validateParams(
        result: PaymentUriParser.ParseResult,
        unconsumedParams: Map<String, String>,
        memo: Pair<String, String>?,
        matchingCoins: List<CryptoCurrency.Coin>,
    ): PaymentUriParser.ParseResult {
        if (result !is PaymentUriParser.ParseResult.Success) return result

        val unsupported = buildMap {
            putAll(unconsumedParams)
            if (memo != null && !isMemoSupported(matchingCoins)) {
                put(memo.first, memo.second)
            }
        }

        return if (unsupported.isEmpty()) {
            result
        } else {
            PaymentUriParser.ParseResult.SuccessWithWarning(result.content, unsupported)
        }
    }

    private fun isMemoSupported(matchingCoins: List<CryptoCurrency.Coin>): Boolean {
        return matchingCoins.any {
            it.network.transactionExtrasType != Network.TransactionExtrasType.NONE
        }
    }

    data class ResolveContext(
        val parsed: QrSentUriParser.Result,
        val matchingCoins: List<CryptoCurrency.Coin>,
        val matchingNetworkIds: Set<Network.ID>,
        val allCurrencies: List<CryptoCurrency>,
        val blockchainName: String,
        val qrCode: String,
    )
}