package com.tangem.feature.wallet.presentation.wallet.qr

import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import java.net.URLDecoder

internal class QrContentClassifier(
    private val blockchainDataProvider: BlockchainDataProvider,
) {

    fun classify(qrCode: String, userCurrencies: List<CryptoCurrency>): ClassifiedQrContent {
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
            val address = withoutScheme.takeWhile {
                it != CHAIN_DELIMITER && it != FUNCTION_DELIMITER && it != PARAM_DELIMITER
            }
            val params = extractParameters(withoutScheme)

            if (address.isBlank()) return@firstNotNullOfOrNull null

            val amount = params[PARAM_AMOUNT]?.toBigDecimalOrNull()
            val memo = (params[PARAM_MEMO] ?: params[PARAM_MESSAGE])?.let {
                runCatching { URLDecoder.decode(it, CHARSET_UTF8) }.getOrDefault(it)
            }

            ClassifiedQrContent.PaymentUri(
                currency = coin,
                address = address,
                amount = amount,
                memo = memo,
            )
        }
    }

    private fun isDAppWcUrl(qrCode: String): Boolean {
        if (!qrCode.startsWith(HTTP_PREFIX) && !qrCode.startsWith(HTTPS_PREFIX)) return false

        val uriParam = extractParameters(qrCode)[PARAM_URI] ?: return false
        val decodedUri = runCatching { URLDecoder.decode(uriParam, CHARSET_UTF8) }.getOrDefault(uriParam)
        return decodedUri.startsWith(WC_PREFIX)
    }

    private fun extractParameters(from: String): Map<String, String> {
        val paramsBlock = from.substringAfter(PARAM_DELIMITER, missingDelimiterValue = "")
        if (paramsBlock.isBlank()) return emptyMap()

        return paramsBlock.split(PARAMS_DELIMITER)
            .mapNotNull { param ->
                val parts = param.split(PARAM_VALUE_DELIMITER, limit = 2)
                if (parts.size == 2) parts[0].lowercase() to parts[1] else null
            }
            .toMap()
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
        const val CHAIN_DELIMITER = '@'
        const val FUNCTION_DELIMITER = '/'
        const val PARAM_DELIMITER = '?'
        const val PARAMS_DELIMITER = '&'
        const val PARAM_VALUE_DELIMITER = '='
        const val PARAM_AMOUNT = "amount"
        const val PARAM_MEMO = "memo"
        const val PARAM_MESSAGE = "message"
        const val CHARSET_UTF8 = "UTF-8"
    }
}