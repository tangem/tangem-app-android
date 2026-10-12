package com.tangem.data.qrscanning.parser

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.qrscanning.models.ClassifiedQrContent
import java.net.URLDecoder

internal class QrContentClassifierParser(
    private val blockchainDataProvider: BlockchainDataProvider,
    private val paymentUriParsers: Set<PaymentUriParser>,
) {

    fun parse(qrCode: String, userCurrencies: List<CryptoCurrency>): ClassifiedQrContent {
        if (qrCode.startsWith(WC_PREFIX)) {
            return ClassifiedQrContent.WalletConnect(qrCode)
        }

        if (isDAppWcUrl(qrCode)) {
            return ClassifiedQrContent.WalletConnect(qrCode)
        }

        parseMcpUserCode(qrCode)?.let { userCode ->
            return ClassifiedQrContent.McpAuthorization(userCode)
        }

        val coins = userCurrencies.filterIsInstance<CryptoCurrency.Coin>()
        val uniqueCoins = coins.distinctBy { it.network.id }

        when (val paymentUriResult = tryParsePaymentUri(qrCode, uniqueCoins, userCurrencies)) {
            is PaymentUriParser.ParseResult.Success -> return paymentUriResult.content
            is PaymentUriParser.ParseResult.SuccessWithWarning -> {
                return ClassifiedQrContent.PaymentUriWarning(
                    paymentUri = paymentUriResult.content,
                    unsupportedParams = paymentUriResult.unsupportedParams,
                )
            }
            is PaymentUriParser.ParseResult.RecognizedError -> return paymentUriResult.error
            is PaymentUriParser.ParseResult.NotRecognized -> Unit
        }

        val matchingCoins = uniqueCoins
            .filter { coin -> blockchainDataProvider.validateAddress(coin.network, qrCode) }
        val matchingNetworkIds = matchingCoins.map { it.network.id }.toSet()

        if (matchingNetworkIds.isNotEmpty()) {
            val matchingCurrencies = userCurrencies.filter { it.network.id in matchingNetworkIds }

            return ClassifiedQrContent.PlainAddress(
                address = qrCode,
                matchingCurrencies = matchingCurrencies,
            )
        }

        val supportedBlockchain = blockchainDataProvider.findSupportedBlockchainName(qrCode)
        if (supportedBlockchain != null) {
            return ClassifiedQrContent.Error.UnsupportedNetwork(
                raw = qrCode,
                blockchain = supportedBlockchain,
            )
        }

        return ClassifiedQrContent.Error.Unrecognized(qrCode)
    }

    private fun tryParsePaymentUri(
        qrCode: String,
        coins: List<CryptoCurrency.Coin>,
        allCurrencies: List<CryptoCurrency>,
    ): PaymentUriParser.ParseResult {
        return paymentUriParsers.firstNotNullOfOrNull { parser ->
            parser.parse(qrCode, coins, allCurrencies).takeUnless { it is PaymentUriParser.ParseResult.NotRecognized }
        } ?: PaymentUriParser.ParseResult.NotRecognized
    }

    /**
     * The MCP gateway encodes the pairing link into a QR as well as into a deep link, so the same URI arrives here
     * unparsed. The prefix and the parameter mirror `DeepLinkRoute.McpAuthorization` / `DeeplinkConst.USER_CODE_KEY`,
     * which the data layer cannot reference without depending on the routing module.
     */
    private fun parseMcpUserCode(qrCode: String): String? {
        if (!qrCode.startsWith(MCP_AUTHORIZATION_PREFIX)) return null

        // Without this the prefix alone would also claim a longer host, e.g. `tangem://mcp.authorizationfoo`
        val afterHost = qrCode.removePrefix(MCP_AUTHORIZATION_PREFIX)
        if (afterHost.isNotEmpty() && !afterHost.startsWith('?')) return null

        val userCode = QrSentUriParser().extractParameters(qrCode)[PARAM_USER_CODE] ?: return null
        val decoded = runCatching {
            URLDecoder.decode(userCode, QrSentUriParser.CHARSET_UTF8)
        }.getOrDefault(userCode)

        return decoded.ifBlank { null }
    }

    private fun isDAppWcUrl(qrCode: String): Boolean {
        if (!qrCode.startsWith(HTTP_PREFIX) && !qrCode.startsWith(HTTPS_PREFIX)) return false

        val uriParser = QrSentUriParser()
        val uriParam = uriParser.extractParameters(qrCode)[PARAM_URI] ?: return false
        val decodedUri = runCatching { URLDecoder.decode(
            uriParam,
            QrSentUriParser.CHARSET_UTF8,
        ) }.getOrDefault(uriParam)
        return decodedUri.startsWith(WC_PREFIX)
    }

    internal interface BlockchainDataProvider {
        fun validateAddress(network: Network, address: String): Boolean
        fun getChainId(network: Network): Long?
        fun findSupportedBlockchainName(address: String): String?
        fun getBlockchainNameByChainId(chainId: Long): String?
    }

    internal class DefaultBlockchainDataProvider : BlockchainDataProvider {
        override fun validateAddress(network: Network, address: String): Boolean {
            return runCatching { network.toBlockchain().validateAddress(address) }.getOrDefault(false)
        }

        override fun getChainId(network: Network): Long? {
            return runCatching { network.toBlockchain().getChainId()?.toLong() }.getOrNull()
        }

        override fun findSupportedBlockchainName(address: String): String? {
            return Blockchain.entries
                .filter { !it.isTestnet() }
                .firstOrNull { blockchain ->
                    runCatching { blockchain.validateAddress(address) }.getOrDefault(false)
                }?.fullName
        }

        override fun getBlockchainNameByChainId(chainId: Long): String? {
            return Blockchain.entries
                .filter { !it.isTestnet() }
                .firstOrNull { blockchain ->
                    runCatching { blockchain.getChainId()?.toLong() == chainId }.getOrDefault(false)
                }?.fullName
        }
    }

    private companion object {
        const val HTTP_PREFIX = "http://"
        const val HTTPS_PREFIX = "https://"
        const val PARAM_URI = "uri"
        const val WC_PREFIX = "wc:"
        const val MCP_AUTHORIZATION_PREFIX = "tangem://mcp.authorization"
        const val PARAM_USER_CODE = "user_code"
    }
}