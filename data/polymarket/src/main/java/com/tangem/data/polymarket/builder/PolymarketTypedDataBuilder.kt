package com.tangem.data.polymarket.builder

import com.tangem.domain.polymarket.approval.PolymarketContracts
import com.tangem.domain.polymarket.model.PolymarketApprovalCall
import org.json.JSONArray
import org.json.JSONObject

/**
 * Builds EIP-712 typed-data JSON for the two Polymarket onboarding signatures: `ClobAuth` (L1 API auth)
 * and the `DepositWallet` approvals `Batch`. The output JSON is consumed by `EthereumUtils.makeTypedDataHash`,
 * the same contract as `Eip712TypedDataBuilder`.
 *
 * Note the two `EIP712Domain` type arrays differ: `ClobAuth` has NO `verifyingContract`, the batch does.
 */
object PolymarketTypedDataBuilder {

    private const val MSG_TO_SIGN = "This message attests that I control the given wallet"

    fun buildClobAuth(address: String, timestamp: String, nonce: String = "0", message: String = MSG_TO_SIGN): String {
        return JSONObject().apply {
            put("types", buildClobAuthTypes())
            put("primaryType", "ClobAuth")
            put("domain", buildClobAuthDomain())
            put(
                "message",
                buildClobAuthMessage(address = address, timestamp = timestamp, nonce = nonce, message = message),
            )
        }.toString()
    }

    fun buildApprovalsBatch(
        depositWallet: String,
        nonce: String,
        deadline: String,
        calls: List<PolymarketApprovalCall>,
    ): String {
        return JSONObject().apply {
            put("types", buildBatchTypes())
            put("primaryType", "Batch")
            put("domain", buildBatchDomain(depositWallet))
            put(
                "message",
                buildBatchMessage(depositWallet = depositWallet, nonce = nonce, deadline = deadline, calls = calls),
            )
        }.toString()
    }

    private fun buildClobAuthTypes(): JSONObject = JSONObject().apply {
        put("EIP712Domain", domainTypeProperties(includeVerifyingContract = false))
        put("ClobAuth", clobAuthFields())
    }

    private fun clobAuthFields(): JSONArray = JSONArray().apply {
        put(typeProperty("address", "address"))
        put(typeProperty("timestamp", "string"))
        put(typeProperty("nonce", "uint256"))
        put(typeProperty("message", "string"))
    }

    private fun buildClobAuthDomain(): JSONObject = JSONObject().apply {
        put("name", "ClobAuthDomain")
        put("version", "1")
        put("chainId", PolymarketContracts.CHAIN_ID)
    }

    private fun buildClobAuthMessage(address: String, timestamp: String, nonce: String, message: String): JSONObject =
        JSONObject().apply {
            put("address", address)
            put("timestamp", timestamp)
            put("nonce", nonce)
            put("message", message)
        }

    private fun buildBatchTypes(): JSONObject = JSONObject().apply {
        put("EIP712Domain", domainTypeProperties(includeVerifyingContract = true))
        put("Batch", batchFields())
        put("Call", callFields())
    }

    private fun batchFields(): JSONArray = JSONArray().apply {
        put(typeProperty("wallet", "address"))
        put(typeProperty("nonce", "uint256"))
        put(typeProperty("deadline", "uint256"))
        put(typeProperty("calls", "Call[]"))
    }

    private fun callFields(): JSONArray = JSONArray().apply {
        put(typeProperty("target", "address"))
        put(typeProperty("value", "uint256"))
        put(typeProperty("data", "bytes"))
    }

    private fun buildBatchDomain(depositWallet: String): JSONObject = JSONObject().apply {
        put("name", "DepositWallet")
        put("version", "1")
        put("chainId", PolymarketContracts.CHAIN_ID)
        put("verifyingContract", depositWallet)
    }

    private fun buildBatchMessage(
        depositWallet: String,
        nonce: String,
        deadline: String,
        calls: List<PolymarketApprovalCall>,
    ): JSONObject = JSONObject().apply {
        put("wallet", depositWallet)
        put("nonce", nonce)
        put("deadline", deadline)
        put("calls", callsArray(calls))
    }

    private fun callsArray(calls: List<PolymarketApprovalCall>): JSONArray = JSONArray().apply {
        calls.forEach { call -> put(callJson(call)) }
    }

    private fun callJson(call: PolymarketApprovalCall): JSONObject = JSONObject().apply {
        put("target", call.target)
        put("value", call.value)
        put("data", call.data)
    }

    private fun domainTypeProperties(includeVerifyingContract: Boolean): JSONArray = JSONArray().apply {
        put(typeProperty("name", "string"))
        put(typeProperty("version", "string"))
        put(typeProperty("chainId", "uint256"))
        if (includeVerifyingContract) put(typeProperty("verifyingContract", "address"))
    }

    private fun typeProperty(name: String, type: String): JSONObject = JSONObject().apply {
        put("name", name)
        put("type", type)
    }
}