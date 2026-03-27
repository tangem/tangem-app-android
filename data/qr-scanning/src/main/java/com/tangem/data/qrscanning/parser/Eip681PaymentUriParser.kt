package com.tangem.data.qrscanning.parser

import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.qrscanning.models.ClassifiedQrContent
import java.math.BigDecimal
import java.math.MathContext

internal class Eip681PaymentUriParser(
    private val blockchainDataProvider: QrContentClassifierParser.BlockchainDataProvider,
) : PaymentUriParser {

    override fun parse(
        qrCode: String,
        coins: List<CryptoCurrency.Coin>,
        allCurrencies: List<CryptoCurrency>,
    ): PaymentUriParser.ParseResult {
        val scheme = SCHEMES.find { qrCode.startsWith(it, ignoreCase = true) }
            ?: return PaymentUriParser.ParseResult.NotRecognized

        val withoutScheme = qrCode.removePrefix(scheme)
        val parsed = parseEip681(withoutScheme) ?: return PaymentUriParser.ParseResult.NotRecognized

        val matchingCoins = findMatchingCoins(parsed.chainId, coins)
        if (matchingCoins.isEmpty()) {
            return PaymentUriParser.ParseResult.RecognizedError(
                ClassifiedQrContent.Error.UnsupportedNetwork(
                    raw = qrCode,
                    blockchain = parsed.chainId?.let { blockchainDataProvider.getBlockchainNameByChainId(it) },
                ),
            )
        }

        val isAddressValid = matchingCoins.any { coin ->
            blockchainDataProvider.validateAddress(coin.network, parsed.targetAddress)
        }
        if (!isAddressValid) {
            return PaymentUriParser.ParseResult.RecognizedError(
                ClassifiedQrContent.Error.Unrecognized(qrCode),
            )
        }

        val result = if (parsed.functionName == FUNCTION_TRANSFER) {
            val recipient = parsed.params[Param.ADDRESS.key]
            if (recipient == null) {
                return PaymentUriParser.ParseResult.RecognizedError(
                    ClassifiedQrContent.Error.Unrecognized(qrCode),
                )
            }
            val isRecipientValid = matchingCoins.any { coin ->
                blockchainDataProvider.validateAddress(coin.network, recipient)
            }
            if (!isRecipientValid) {
                return PaymentUriParser.ParseResult.RecognizedError(
                    ClassifiedQrContent.Error.Unrecognized(qrCode),
                )
            }
            resolveErc20Transfer(parsed, matchingCoins, allCurrencies)
        } else {
            resolveNativeTransfer(parsed, matchingCoins, allCurrencies)
        }
        if (result == null) {
            return PaymentUriParser.ParseResult.RecognizedError(
                ClassifiedQrContent.Error.UnsupportedNetwork(
                    raw = qrCode,
                    blockchain = matchingCoins.firstOrNull()?.network?.name,
                ),
            )
        }

        val unsupportedParams = findUnsupportedParams(parsed.params, parsed.functionName)
        return if (unsupportedParams.isEmpty()) {
            PaymentUriParser.ParseResult.Success(result)
        } else {
            PaymentUriParser.ParseResult.SuccessWithWarning(result, unsupportedParams)
        }
    }

    private fun findUnsupportedParams(params: Map<String, String>, functionName: String?): Map<String, String> {
        val supportedKeys = if (functionName == FUNCTION_TRANSFER) {
            Param.transferParams()
        } else {
            Param.nativeParams()
        }
        return params.filterKeys { key -> key !in supportedKeys }
    }

    private fun resolveNativeTransfer(
        parsed: Eip681Result,
        matchingCoins: List<CryptoCurrency.Coin>,
        allCurrencies: List<CryptoCurrency>,
    ): ClassifiedQrContent.PaymentUri? {
        val valueWei = parsed.params[Param.VALUE.key]?.toBigDecimalOrNull()

        if (matchingCoins.isEmpty()) return null

        val decimals = matchingCoins.first().decimals
        val amount = valueWei?.fromSmallestUnit(decimals)

        val matchingNetworkIds = matchingCoins.map { it.network.id }.toSet()
        // If value is specified, this is a native coin transfer — return only coins
        // If no value, it's just an address with scheme — return all currencies on the network
        val matchingCurrencies = if (valueWei != null) {
            matchingCoins
        } else {
            allCurrencies.filter { it.network.id in matchingNetworkIds }
        }

        return ClassifiedQrContent.PaymentUri(
            address = parsed.targetAddress,
            amount = amount,
            memo = null,
            matchingCurrencies = matchingCurrencies,
        )
    }

    private fun resolveErc20Transfer(
        parsed: Eip681Result,
        matchingCoins: List<CryptoCurrency.Coin>,
        allCurrencies: List<CryptoCurrency>,
    ): ClassifiedQrContent.PaymentUri? {
        val recipient = parsed.params[Param.ADDRESS.key] ?: return null
        val contractAddress = parsed.targetAddress

        val matchingNetworkIds = matchingCoins.map { it.network.id }.toSet()

        val matchingTokens = allCurrencies.filterIsInstance<CryptoCurrency.Token>()
            .filter { token ->
                token.network.id in matchingNetworkIds &&
                    token.contractAddress.equals(contractAddress, ignoreCase = true)
            }

        if (matchingTokens.isEmpty()) return null

        val rawAmount = parsed.params[Param.UINT256.key]?.toBigDecimalOrNull()
        val amount = rawAmount?.fromSmallestUnit(matchingTokens.first().decimals)

        return ClassifiedQrContent.PaymentUri(
            address = recipient,
            amount = amount,
            memo = null,
            matchingCurrencies = matchingTokens,
        )
    }

    private fun findMatchingCoins(chainId: Long?, coins: List<CryptoCurrency.Coin>): List<CryptoCurrency.Coin> {
        if (chainId == null) {
            return coins.filter { coin ->
                blockchainDataProvider.getChainId(coin.network) != null
            }
        }
        return coins.filter { coin ->
            blockchainDataProvider.getChainId(coin.network) == chainId
        }
    }

    private fun parseEip681(withoutScheme: String): Eip681Result? {
        val match = URI_REGEX.matchEntire(withoutScheme) ?: return null

        val targetAddress = match.groupValues[GROUP_ADDRESS].ifBlank { return null }
        val pathChainId = match.groupValues[GROUP_CHAIN_ID].toLongOrNull()
        val functionName = match.groupValues[GROUP_FUNCTION].ifBlank { null }
        val queryString = match.groupValues[GROUP_QUERY]

        val params = parseQueryParams(queryString)
        val chainId = pathChainId ?: params[Param.CHAIN_ID.key]?.toLongOrNull()

        return Eip681Result(
            targetAddress = targetAddress,
            chainId = chainId,
            functionName = functionName,
            params = params,
        )
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        return query.split('&').mapNotNull { param ->
            val parts = param.split('=', limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else null
        }.toMap()
    }

    private fun BigDecimal.fromSmallestUnit(decimals: Int): BigDecimal {
        if (decimals == 0) return this
        return this.divide(BigDecimal.TEN.pow(decimals), MathContext.DECIMAL128)
    }

    private data class Eip681Result(
        val targetAddress: String,
        val chainId: Long?,
        val functionName: String?,
        val params: Map<String, String>,
    )

    private enum class Param(val key: String) {
        VALUE("value"),
        ADDRESS("address"),
        UINT256("uint256"),
        CHAIN_ID("chainId"),
        ;

        companion object {
            /** Params supported for native transfers (no function or unknown function). */
            fun nativeParams(): Set<String> = setOf(VALUE.key, CHAIN_ID.key)

            /** Params supported for ERC-20 transfer() calls. */
            fun transferParams(): Set<String> = setOf(ADDRESS.key, UINT256.key, CHAIN_ID.key)
        }
    }

    private companion object {
        // ethereum:<address>[@<chainId>][/<function>][?<params>]
        val URI_REGEX = Regex("""^([^@/?]+)(?:@(\d+))?(?:/([^?]+))?(?:\?(.+))?$""")
        val SCHEMES = Blockchain.Ethereum.getShareScheme()
        const val FUNCTION_TRANSFER = "transfer"
        const val GROUP_ADDRESS = 1
        const val GROUP_CHAIN_ID = 2
        const val GROUP_FUNCTION = 3
        const val GROUP_QUERY = 4
    }
}