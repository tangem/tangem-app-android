package com.tangem.data.jointaccount.safe

import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.ADDRESS_HEX_LENGTH
import com.tangem.blockchain.common.HEX_PREFIX
import com.tangem.blockchain.extensions.isValidHex
import com.tangem.domain.jointaccount.model.SafeTx
import java.math.BigInteger

/**
 * The `eth_signTypedData_v4` payload of a [SafeTx] and the EIP-712 digest of that payload.
 *
 * The digest is hashed by the blockchain SDK out of the very JSON the Blockaid gate is shown, so the bytes the card
 * signs and the payload the user is vetted against cannot drift apart. What stays here is the Safe v1.5.0 schema —
 * the type strings and the field order — which is the part no generic EIP-712 encoder can know.
 */
internal object SafeTxTypedDataBuilder {

    private val UINT8_RANGE = 0..255
    private const val UINT256_BITS = 256

    fun digest(safeTx: SafeTx, chainId: Long, verifyingContract: String): ByteArray {
        val json = buildJson(safeTx = safeTx, chainId = chainId, verifyingContract = verifyingContract)

        return EthereumUtils.makeTypedDataHash(rawMessage = json)
    }

    fun buildJson(safeTx: SafeTx, chainId: Long, verifyingContract: String): String {
        requireHashable(safeTx = safeTx, chainId = chainId, verifyingContract = verifyingContract)

        return buildString {
            append("""{"types":{"EIP712Domain":[""")
            append("""{"name":"chainId","type":"uint256"},{"name":"verifyingContract","type":"address"}""")
            append("""],"SafeTx":[""")
            append("""{"name":"to","type":"address"},{"name":"value","type":"uint256"},""")
            append("""{"name":"data","type":"bytes"},{"name":"operation","type":"uint8"},""")
            append("""{"name":"safeTxGas","type":"uint256"},{"name":"baseGas","type":"uint256"},""")
            append("""{"name":"gasPrice","type":"uint256"},{"name":"gasToken","type":"address"},""")
            append("""{"name":"refundReceiver","type":"address"},{"name":"nonce","type":"uint256"}""")
            append("""]},"primaryType":"SafeTx","domain":{""")
            append(""""chainId":$chainId,"verifyingContract":"${verifyingContract.lowercase()}"""")
            append("""},"message":{""")
            append(""""to":"${safeTx.to.lowercase()}",""")
            append(""""value":"${safeTx.value}",""")
            append(""""data":"${normalizedData(safeTx.data)}",""")
            append(""""operation":${safeTx.operation},""")
            append(""""safeTxGas":"${safeTx.safeTxGas}",""")
            append(""""baseGas":"${safeTx.baseGas}",""")
            append(""""gasPrice":"${safeTx.gasPrice}",""")
            append(""""gasToken":"${safeTx.gasToken.lowercase()}",""")
            append(""""refundReceiver":"${safeTx.refundReceiver.lowercase()}",""")
            append(""""nonce":"${safeTx.nonce}"""")
            append("}}")
        }
    }

    /**
     * Refuses a payload the Safe schema cannot hold before it reaches the encoder: an out-of-range number or a
     * malformed address would otherwise be silently padded or truncated into a different transaction.
     */
    private fun requireHashable(safeTx: SafeTx, chainId: Long, verifyingContract: String) {
        requireAddress(name = "verifyingContract", address = verifyingContract)
        requireAddress(name = "to", address = safeTx.to)
        requireAddress(name = "gasToken", address = safeTx.gasToken)
        requireAddress(name = "refundReceiver", address = safeTx.refundReceiver)

        require(chainId > 0) { "chainId is a positive uint256 in the EIP-712 domain, got $chainId" }
        require(safeTx.operation in UINT8_RANGE) { "operation is a uint8 in the Safe schema, got ${safeTx.operation}" }
        requireUint256(name = "value", value = safeTx.value)
        requireUint256(name = "safeTxGas", value = safeTx.safeTxGas)
        requireUint256(name = "baseGas", value = safeTx.baseGas)
        requireUint256(name = "gasPrice", value = safeTx.gasPrice)
        requireUint256(name = "nonce", value = safeTx.nonce)

        val payload = safeTx.data.withoutHexPrefix()
        require(payload.isValidHex()) { "data is a whole number of hex bytes in the Safe schema" }
    }

    private fun requireAddress(name: String, address: String) {
        val hex = address.withoutHexPrefix()
        require(hex.length == ADDRESS_HEX_LENGTH && hex.isValidHex()) { "$name is not a 20-byte hex address" }
    }

    private fun requireUint256(name: String, value: BigInteger) {
        require(value.signum() >= 0 && value.bitLength() <= UINT256_BITS) { "$name does not fit a uint256" }
    }

    private fun normalizedData(data: String): String = HEX_PREFIX + data.withoutHexPrefix().lowercase()

    private fun String.withoutHexPrefix(): String =
        if (startsWith(prefix = HEX_PREFIX, ignoreCase = true)) drop(n = HEX_PREFIX.length) else this
}