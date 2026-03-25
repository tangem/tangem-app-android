package com.tangem.data.qrscanning.parser

import java.math.BigDecimal
import java.net.URLDecoder

internal class QrSentUriParser {

    data class Result(
        val address: String,
        val amount: BigDecimal?,
        val memo: String?,
        val params: Map<String, String>,
    )

    fun parse(withoutScheme: String): Result? {
        val address = withoutScheme.takeWhile {
            it != CHAIN_DELIMITER && it != FUNCTION_DELIMITER && it != PARAM_DELIMITER
        }
        if (address.isBlank()) return null

        val params = extractParameters(withoutScheme)
        val amount = params[PARAM_AMOUNT]?.toBigDecimalOrNull()
        val memo = (params[PARAM_MEMO] ?: params[PARAM_MESSAGE] ?: params[PARAM_DT])?.let {
            runCatching { URLDecoder.decode(it, CHARSET_UTF8) }.getOrDefault(it)
        }

        return Result(
            address = address,
            amount = amount,
            memo = memo,
            params = params,
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

    companion object {
        const val CHAIN_DELIMITER = '@'
        const val FUNCTION_DELIMITER = '/'
        const val PARAM_DELIMITER = '?'
        const val PARAMS_DELIMITER = '&'
        const val PARAM_VALUE_DELIMITER = '='
        const val PARAM_AMOUNT = "amount"
        const val PARAM_MEMO = "memo"
        const val PARAM_MESSAGE = "message"
        const val PARAM_DT = "dt"
        const val PARAM_ADDRESS = "address"
        const val PARAM_VALUE = "value"
        const val PARAM_UINT256 = "uint256"
        const val CHARSET_UTF8 = "UTF-8"
    }
}