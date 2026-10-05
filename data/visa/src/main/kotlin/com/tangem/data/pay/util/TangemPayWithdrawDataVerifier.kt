package com.tangem.data.pay.util

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.common.HEX_PREFIX
import com.tangem.common.extensions.toHexString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.math.BigDecimal

/**
 * Tells whether the withdraw data the backend asks the card to sign describes the transfer the app requested.
 *
 * The card signs 32 opaque bytes, so this is the only moment the app can tell what it is about to authorise: the
 * hash has to be the EIP-712 digest of the typed data, and the typed data has to move the requested amount of the
 * requested token to the requested recipient.
 */
internal object TangemPayWithdrawDataVerifier {

    private const val CENTS_SCALE = 2
    private const val TYPES_KEY = "types"
    private const val PRIMARY_TYPE_KEY = "primaryType"
    private const val DOMAIN_TYPE = "EIP712Domain"
    private const val NAME_KEY = "name"
    private const val DOMAIN_KEY = "domain"
    private const val MESSAGE_KEY = "message"
    private const val CHAIN_ID_KEY = "chainId"
    private const val ASSET_KEY = "asset"
    private const val RECIPIENT_KEY = "recipient"
    private const val AMOUNT_KEY = "amount"

    /**
     * [recipientAddress] is `null` when the provider picks the recipient (bank off-ramp); [chainId] and
     * [tokenContractAddress] are `null` for a withdrawal from the account's default network.
     */
    data class Expectation(
        val amountInCents: BigDecimal,
        val tokenDecimals: Int,
        val recipientAddress: String?,
        val chainId: Long?,
        val tokenContractAddress: String?,
    )

    enum class Mismatch { NO_STRUCTURED_DATA, MALFORMED_STRUCTURED_DATA, NETWORK, RECIPIENT, AMOUNT, HASH }

    fun verify(hash: String, structuredData: String?, expectation: Expectation): Either<Mismatch, Unit> = either {
        ensureNotNull(structuredData) { Mismatch.NO_STRUCTURED_DATA }
        val typedData = ensureNotNull(parse(structuredData)) { Mismatch.MALFORMED_STRUCTURED_DATA }
        val domain = typedData.signedValues(valuesKey = DOMAIN_KEY, typeName = DOMAIN_TYPE)
        val message = typedData.signedValues(valuesKey = MESSAGE_KEY, typeName = typedData.primitive(PRIMARY_TYPE_KEY))

        if (expectation.chainId != null) {
            ensure(domain[CHAIN_ID_KEY]?.toLongOrNull() == expectation.chainId) { Mismatch.NETWORK }
        }
        if (expectation.tokenContractAddress != null) {
            val asset = message[ASSET_KEY]
            ensure(asset.equals(expectation.tokenContractAddress, ignoreCase = true)) { Mismatch.NETWORK }
        }
        if (expectation.recipientAddress != null) {
            val recipient = message[RECIPIENT_KEY]
            ensure(recipient.equals(expectation.recipientAddress, ignoreCase = true)) { Mismatch.RECIPIENT }
        }
        val amount = message[AMOUNT_KEY]?.toBigDecimalOrNull()
        ensure(amount != null && amount.compareTo(expectation.tokenAmount()) == 0) { Mismatch.AMOUNT }
        ensure(isDigestOf(hash = hash, structuredData = structuredData)) { Mismatch.HASH }
    }

    private fun Expectation.tokenAmount(): BigDecimal =
        amountInCents.movePointLeft(CENTS_SCALE).movePointRight(tokenDecimals)

    private fun parse(structuredData: String): JsonObject? =
        runCatching { Json.parseToJsonElement(structuredData) as? JsonObject }.getOrNull()

    private fun JsonObject.signedValues(valuesKey: String, typeName: String?): Map<String, String?> {
        val values = get(valuesKey) as? JsonObject
        val fields = (get(TYPES_KEY) as? JsonObject)?.get(typeName.orEmpty()) as? JsonArray
        return fields.orEmpty()
            .mapNotNull { field -> (field as? JsonObject)?.primitive(NAME_KEY) }
            .associateWith { name -> values?.primitive(name) }
    }

    private fun JsonObject.primitive(key: String): String? = (get(key) as? JsonPrimitive)?.contentOrNull

    private fun isDigestOf(hash: String, structuredData: String): Boolean = runCatching {
        EthereumUtils.makeTypedDataHash(rawMessage = structuredData)
            .toHexString()
            .equals(hash.withoutHexPrefix(), ignoreCase = true)
    }.getOrDefault(defaultValue = false)

    private fun String.withoutHexPrefix(): String =
        if (startsWith(prefix = HEX_PREFIX, ignoreCase = true)) drop(n = HEX_PREFIX.length) else this
}