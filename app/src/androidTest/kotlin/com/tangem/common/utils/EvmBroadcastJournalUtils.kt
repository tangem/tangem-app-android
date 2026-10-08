package com.tangem.common.utils

import java.math.BigInteger

private const val SEND_RAW_TRANSACTION_METHOD = "eth_sendRawTransaction"
private const val APPROVE_SELECTOR = "095ea7b3"
private const val HEX_RADIX = 16

val UNLIMITED_APPROVE_AMOUNT: BigInteger = BigInteger.ONE.shiftLeft(256).subtract(BigInteger.ONE)

fun findBroadcastTransactions(rpcPathPattern: String): List<String> =
    findWireMockRequestBodiesByPathPattern(method = "POST", urlPathPattern = rpcPathPattern)
        .filter { body -> body.contains(SEND_RAW_TRANSACTION_METHOD) }

fun findBroadcastApproveAmounts(rpcPathPattern: String, spenderAddress: String): List<BigInteger> {
    val spender = spenderAddress.removePrefix("0x").lowercase()
    val approveCallData = Regex("${APPROVE_SELECTOR}0{24}$spender([0-9a-f]{64})", RegexOption.IGNORE_CASE)
    return findBroadcastTransactions(rpcPathPattern).mapNotNull { body ->
        approveCallData.find(body)?.groupValues?.get(1)?.let { amount -> BigInteger(amount, HEX_RADIX) }
    }
}