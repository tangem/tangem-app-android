package com.tangem.data.polymarket.builder

import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.toKeccak
import com.tangem.domain.polymarket.approval.PolymarketContracts
import com.tangem.domain.polymarket.model.PredictionOrder
import com.tangem.domain.polymarket.model.PredictionOrderSide
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigInteger

object PolymarketOrderTypedDataBuilder {

    private const val ORDER_TYPE = "Order(uint256 salt,address maker,address signer,uint256 tokenId," +
        "uint256 makerAmount,uint256 takerAmount,uint8 side,uint8 signatureType,uint256 timestamp," +
        "bytes32 metadata,bytes32 builder)"

    private const val DOMAIN_TYPE =
        "EIP712Domain(string name,string version,uint256 chainId,address verifyingContract)"

    private const val WORD_SIZE = 32
    private const val ADDRESS_SIZE = 20

    private val EIP191_PREFIX = byteArrayOf(0x19, 0x01)

    fun orderTypeString(): String = ORDER_TYPE

    fun orderTypeHash(): ByteArray = ORDER_TYPE.toByteArray().toKeccak()

    /** The exchange the order is signed against: the only thing a neg-risk market changes. */
    fun exchangeAddress(isNegRisk: Boolean): String = if (isNegRisk) {
        PolymarketContracts.NEG_RISK_CTF_EXCHANGE
    } else {
        PolymarketContracts.CTF_EXCHANGE
    }

    fun domainSeparator(isNegRisk: Boolean): ByteArray = concat(
        DOMAIN_TYPE.toByteArray().toKeccak(),
        PolymarketContracts.EXCHANGE_DOMAIN_NAME.toByteArray().toKeccak(),
        PolymarketContracts.EXCHANGE_DOMAIN_VERSION.toByteArray().toKeccak(),
        word(BigInteger.valueOf(PolymarketContracts.CHAIN_ID)),
        addressWord(exchangeAddress(isNegRisk)),
    ).toKeccak()

    /**
     * Encoded **by hand**, unlike the onboarding signatures, which go through a typed-data JSON encoder:
     * all eleven fields are static types, so the hash is just `keccak(typehash ‖ 11 words)`, and doing it
     * directly removes any dependence on how a third-party encoder treats `bytes32` and `uint8`.
     * [buildOrderJson] keeps the JSON path alive purely so the test can cross-check the two.
     */
    fun hashStruct(order: PredictionOrder): ByteArray = concat(
        orderTypeHash(),
        word(BigInteger.valueOf(order.salt)),
        addressWord(order.maker),
        addressWord(order.signer),
        word(order.tokenId),
        word(order.makerAmount),
        word(order.takerAmount),
        word(BigInteger.valueOf(order.side.wireValue)),
        word(BigInteger.valueOf(order.signatureType.toLong())),
        word(BigInteger.valueOf(order.timestampMillis)),
        bytes32Word(order.metadata),
        bytes32Word(order.builder),
    ).toKeccak()

    fun digest(order: PredictionOrder, isNegRisk: Boolean): ByteArray =
        concat(EIP191_PREFIX, domainSeparator(isNegRisk), hashStruct(order)).toKeccak()

    /** Only for the digest cross-check in tests: production hashes the struct directly. */
    fun buildOrderJson(order: PredictionOrder, isNegRisk: Boolean): String = JSONObject().apply {
        put("types", orderTypes())
        put("primaryType", "Order")
        put("domain", orderDomain(isNegRisk = isNegRisk))
        put("message", orderMessage(order))
    }.toString()

    private fun orderTypes(): JSONObject = JSONObject().apply {
        put("EIP712Domain", domainTypeFields())
        put("Order", orderTypeFields())
    }

    private fun orderDomain(isNegRisk: Boolean): JSONObject = JSONObject().apply {
        put("name", PolymarketContracts.EXCHANGE_DOMAIN_NAME)
        put("version", PolymarketContracts.EXCHANGE_DOMAIN_VERSION)
        put("chainId", PolymarketContracts.CHAIN_ID)
        put("verifyingContract", exchangeAddress(isNegRisk))
    }

    private fun orderMessage(order: PredictionOrder): JSONObject = JSONObject().apply {
        put("salt", order.salt.toString())
        put("maker", order.maker)
        put("signer", order.signer)
        put("tokenId", order.tokenId.toString())
        put("makerAmount", order.makerAmount.toString())
        put("takerAmount", order.takerAmount.toString())
        put("side", order.side.wireValue.toString())
        put("signatureType", order.signatureType.toString())
        put("timestamp", order.timestampMillis.toString())
        put("metadata", order.metadata)
        put("builder", order.builder)
    }

    private fun orderTypeFields(): JSONArray = JSONArray().apply {
        put(field(name = "salt", type = "uint256"))
        put(field(name = "maker", type = "address"))
        put(field(name = "signer", type = "address"))
        put(field(name = "tokenId", type = "uint256"))
        put(field(name = "makerAmount", type = "uint256"))
        put(field(name = "takerAmount", type = "uint256"))
        put(field(name = "side", type = "uint8"))
        put(field(name = "signatureType", type = "uint8"))
        put(field(name = "timestamp", type = "uint256"))
        put(field(name = "metadata", type = "bytes32"))
        put(field(name = "builder", type = "bytes32"))
    }

    private fun domainTypeFields(): JSONArray = JSONArray().apply {
        put(field(name = "name", type = "string"))
        put(field(name = "version", type = "string"))
        put(field(name = "chainId", type = "uint256"))
        put(field(name = "verifyingContract", type = "address"))
    }

    private fun field(name: String, type: String): JSONObject = JSONObject().apply {
        put("name", name)
        put("type", type)
    }

    private fun word(value: BigInteger): ByteArray {
        val bytes = value.toByteArray().let { if (it.size > WORD_SIZE) it.copyOfRange(1, it.size) else it }

        return ByteArray(WORD_SIZE - bytes.size) + bytes
    }

    private fun addressWord(address: String): ByteArray {
        val bytes = address.removePrefix("0x").hexToByteArray()
        require(bytes.size == ADDRESS_SIZE) { "Address must be $ADDRESS_SIZE bytes: $address" }

        return ByteArray(WORD_SIZE - ADDRESS_SIZE) + bytes
    }

    private fun bytes32Word(value: String): ByteArray {
        val bytes = value.removePrefix("0x").hexToByteArray()
        require(bytes.size == WORD_SIZE) { "Value must be $WORD_SIZE bytes: $value" }

        return bytes
    }

    private fun String.hexToByteArray(): ByteArray = chunked(size = 2).map { it.toInt(radix = 16).toByte() }
        .toByteArray()

    private fun concat(vararg parts: ByteArray): ByteArray = parts.reduce { acc, part -> acc + part }
}

private val PredictionOrderSide.wireValue: Long
    get() = when (this) {
        PredictionOrderSide.BUY -> 0L
        PredictionOrderSide.SELL -> 1L
    }