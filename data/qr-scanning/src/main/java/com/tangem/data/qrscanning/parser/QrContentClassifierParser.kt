package com.tangem.data.qrscanning.parser

import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.qrscanning.models.ClassifiedQrContent
import java.net.URLDecoder

internal class QrContentClassifierParser(
    private val blockchainDataProvider: BlockchainDataProvider,
    private val paymentUriParser: QrSentUriParser = QrSentUriParser(),
) {

    fun parse(qrCode: String, userCurrencies: List<CryptoCurrency>): ClassifiedQrContent {
        if (qrCode.startsWith(WC_PREFIX)) {
            return ClassifiedQrContent.WalletConnect(qrCode)
        }

        if (isDAppWcUrl(qrCode)) {
            return ClassifiedQrContent.WalletConnect(qrCode)
        }

        val coins = userCurrencies.filterIsInstance<CryptoCurrency.Coin>()
        val uniqueCoins = coins.distinctBy { it.network.id }

        val paymentUri = tryParsePaymentUri(qrCode, uniqueCoins)
        if (paymentUri != null) return paymentUri

        val matchingCurrencies = uniqueCoins.filter { coin ->
            blockchainDataProvider.validateAddress(coin.network, qrCode)
        }

        if (matchingCurrencies.isNotEmpty()) {
            return ClassifiedQrContent.PlainAddress(
                address = qrCode,
                matchingCurrencies = matchingCurrencies,
            )
        }

        return ClassifiedQrContent.Unknown(qrCode)
    }

    private fun tryParsePaymentUri(qrCode: String, coins: List<CryptoCurrency.Coin>): ClassifiedQrContent.PaymentUri? {
        return coins.firstNotNullOfOrNull { coin ->
            val matchedScheme = blockchainDataProvider.getShareSchemes(coin.network)
                .sortedByDescending { it.length }
                .firstOrNull { qrCode.startsWith(it) }
                ?: return@firstNotNullOfOrNull null

            val withoutScheme = qrCode.removePrefix(matchedScheme)
            val parsed = paymentUriParser.parse(withoutScheme) ?: return@firstNotNullOfOrNull null

            ClassifiedQrContent.PaymentUri(
                currency = coin,
                address = parsed.address,
                amount = parsed.amount,
                memo = parsed.memo,
            )
        }
    }

    private fun isDAppWcUrl(qrCode: String): Boolean {
        if (!qrCode.startsWith(HTTP_PREFIX) && !qrCode.startsWith(HTTPS_PREFIX)) return false

        val uriParam = paymentUriParser.extractParameters(qrCode)[PARAM_URI] ?: return false
        val decodedUri = runCatching { URLDecoder.decode(
            uriParam,
            QrSentUriParser.CHARSET_UTF8,
        ) }.getOrDefault(uriParam)
        return decodedUri.startsWith(WC_PREFIX)
    }

    internal interface BlockchainDataProvider {
        fun getShareSchemes(network: Network): List<String>
        fun validateAddress(network: Network, address: String): Boolean
    }

    internal class DefaultBlockchainDataProvider : BlockchainDataProvider {
        override fun getShareSchemes(network: Network): List<String> {
            return runCatching { network.toBlockchain().getShareScheme() }.getOrDefault(emptyList())
        }

        override fun validateAddress(network: Network, address: String): Boolean {
            return runCatching { network.toBlockchain().validateAddress(address) }.getOrDefault(false)
        }
    }

    private companion object {
        const val HTTP_PREFIX = "http://"
        const val HTTPS_PREFIX = "https://"
        const val PARAM_URI = "uri"
        const val WC_PREFIX = "wc:"
    }
}