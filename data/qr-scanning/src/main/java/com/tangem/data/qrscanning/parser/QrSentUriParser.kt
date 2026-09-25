package com.tangem.data.qrscanning.parser

import java.math.BigDecimal
import java.net.URLDecoder

internal class QrSentUriParser {

    data class Result(
        val address: String,
        val amount: BigDecimal?,
        val memo: Pair<String, String>?,
        val remainingParams: Map<String, String>,
        /** ERC-681 `@<chainId>` path segment, `null` when absent. */
        val chainId: Long? = null,
        /** ERC-681 `/<function>` path segment (e.g. `transfer`), `null` when absent. */
        val functionName: String? = null,
    )

    fun parse(withoutScheme: String): Result? {
        val address = withoutScheme.takeWhile {
            it != CHAIN_DELIMITER && it != FUNCTION_DELIMITER && it != PARAM_DELIMITER
        }
        if (address.isBlank()) return null

        // ERC-681 path: <address>[@<chainId>][/<function>][?<params>]. Both segments are part of what the payee
        // asked for and must not be dropped silently: the chain id selects the network, the function tells
        // whether the URI is a plain transfer at all.
        val path = withoutScheme.substringBefore(PARAM_DELIMITER).drop(address.length)
        val chainIdSegment = path.substringAfter(CHAIN_DELIMITER, missingDelimiterValue = "")
            .substringBefore(FUNCTION_DELIMITER)
        val chainId = if (chainIdSegment.isEmpty()) null else chainIdSegment.toLongOrNull() ?: return null
        val functionName = path.substringAfter(FUNCTION_DELIMITER, missingDelimiterValue = "")
            .takeIf(String::isNotEmpty)

        val params = extractParameters(withoutScheme)
        val amount = params[PARAM_AMOUNT]?.toBigDecimalOrNull()

        val memoKey = MemoParam.keys.firstOrNull { it in params }
        val memo = memoKey?.let { key ->
            val raw = params[key] ?: return@let null
            val decoded = runCatching { URLDecoder.decode(raw, CHARSET_UTF8) }.getOrDefault(raw)
            key to decoded
        }

        val consumedKeys = buildSet {
            add(PARAM_AMOUNT)
            addAll(MemoParam.keys)
        }

        return Result(
            address = address,
            amount = amount,
            memo = memo,
            remainingParams = params - consumedKeys,
            chainId = chainId,
            functionName = functionName,
        )
    }

    fun extractParameters(from: String): Map<String, String> {
        val paramsBlock = from.substringAfter(PARAM_DELIMITER, missingDelimiterValue = "")
        if (paramsBlock.isBlank()) return emptyMap()

        return paramsBlock.split(PARAMS_DELIMITER)
            .mapNotNull { param ->
                val parts = param.split(PARAM_VALUE_DELIMITER, limit = 2)
                if (parts.size == 2) parts[0].lowercase() to parts[1] else null
            }
            .toMap()
    }

    enum class MemoParam(val key: String) {
        MEMO("memo"),
        MESSAGE("message"),
        DESTINATION_TAG("dt"),
        ;

        companion object {
            val keys = entries.map { it.key }.toSet()
        }
    }

    companion object {
        const val CHAIN_DELIMITER = '@'
        const val FUNCTION_DELIMITER = '/'
        const val PARAM_DELIMITER = '?'
        const val PARAMS_DELIMITER = '&'
        const val PARAM_VALUE_DELIMITER = '='
        const val PARAM_AMOUNT = "amount"
        const val PARAM_ADDRESS = "address"
        const val PARAM_VALUE = "value"
        const val PARAM_UINT256 = "uint256"
        const val FUNCTION_TRANSFER = "transfer"
        const val CHARSET_UTF8 = "UTF-8"
    }
}