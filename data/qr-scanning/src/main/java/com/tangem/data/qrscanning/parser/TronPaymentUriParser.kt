package com.tangem.data.qrscanning.parser

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.qrscanning.models.ClassifiedQrContent
import java.math.BigDecimal
import java.math.MathContext

internal class TronPaymentUriParser(
    private val blockchainDataProvider: QrContentClassifierParser.BlockchainDataProvider,
    private val helper: PaymentUriResolveHelper = PaymentUriResolveHelper(),
) : PaymentUriParser {

    override fun parse(
        qrCode: String,
        coins: List<CryptoCurrency.Coin>,
        allCurrencies: List<CryptoCurrency>,
    ): PaymentUriParser.ParseResult {
        val scheme = SCHEMES.find { qrCode.startsWith(it, ignoreCase = true) }
            ?: return PaymentUriParser.ParseResult.NotRecognized

        val parsed = helper.parseUri(qrCode, scheme)
            ?: return PaymentUriParser.ParseResult.RecognizedError(
                ClassifiedQrContent.Error.Unrecognized(qrCode),
            )

        val matchingCoins = coins.filter { it.network.toBlockchain() == BLOCKCHAIN }
        if (matchingCoins.isEmpty()) {
            return PaymentUriParser.ParseResult.RecognizedError(
                ClassifiedQrContent.Error.UnsupportedNetwork(raw = qrCode, blockchain = BLOCKCHAIN.fullName),
            )
        }

        val isAddressValid = matchingCoins.any {
            blockchainDataProvider.validateAddress(it.network, parsed.address)
        }
        if (!isAddressValid) {
            return PaymentUriParser.ParseResult.RecognizedError(
                ClassifiedQrContent.Error.Unrecognized(qrCode),
            )
        }

        val matchingNetworkIds = matchingCoins.map { it.network.id }.toSet()
        val context = PaymentUriResolveHelper.ResolveContext(
            parsed = parsed,
            matchingCoins = matchingCoins,
            matchingNetworkIds = matchingNetworkIds,
            allCurrencies = allCurrencies,
            blockchainName = BLOCKCHAIN.fullName,
            qrCode = qrCode,
        )

        val tokenContractAddress = parsed.remainingParams[PARAM_TOKEN]
        val result = if (tokenContractAddress != null) {
            helper.resolveTokenTransfer(context, tokenContractAddress, ::interpretAmount)
        } else {
            helper.resolveNativeOrAll(context, ::interpretAmount)
        }

        val unconsumed = parsed.remainingParams - PARAM_TOKEN
        return helper.validateParams(
            result = result,
            unconsumedParams = unconsumed,
            memo = parsed.memo,
            matchingCoins = matchingCoins,
        )
    }

    /**
     * Tron amount interpretation:
     * - If the number contains a decimal point — use as-is
     * - If no decimal point and <= [AMOUNT_THRESHOLD] — treat as a normal number (e.g. 100 = 100 TRX)
     * - If no decimal point and > [AMOUNT_THRESHOLD] — treat as smallest unit, shift decimal left by [decimals]
     */
    private fun interpretAmount(raw: BigDecimal, decimals: Int): BigDecimal {
        val hasDecimalPoint = raw.scale() > 0
        return if (hasDecimalPoint || raw <= AMOUNT_THRESHOLD) {
            raw
        } else {
            raw.divide(BigDecimal.TEN.pow(decimals), MathContext.DECIMAL128)
        }
    }

    private companion object {
        val BLOCKCHAIN = Blockchain.Tron
        val SCHEMES = BLOCKCHAIN.getShareScheme()
        val AMOUNT_THRESHOLD = BigDecimal(100_000)
        const val PARAM_TOKEN = "token"
    }
}