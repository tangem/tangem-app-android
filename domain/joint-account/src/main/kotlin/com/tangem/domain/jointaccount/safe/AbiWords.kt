package com.tangem.domain.jointaccount.safe

import com.tangem.common.extensions.hexToBytes
import java.math.BigInteger

private const val WORD = 32
private const val ADDRESS_BYTES = 20

/** The left-padded 32-byte ABI word of a uint256 */
internal fun BigInteger.toUint256Word(): ByteArray {
    require(signum() >= 0) { "A uint256 cannot be negative" }
    require(bitLength() <= WORD * Byte.SIZE_BITS) { "A uint256 cannot exceed 256 bits" }
    // toByteArray() may carry a leading sign byte — the only way the array is wider than a word here
    val raw = toByteArray().let { if (it.size > WORD) it.copyOfRange(it.size - WORD, it.size) else it }

    return ByteArray(WORD - raw.size) + raw
}

/** The left-padded 32-byte ABI word of a 20-byte EVM address */
internal fun String.toAddressWord(): ByteArray {
    val hex = removePrefix(prefix = "0x")
    // Checked on the string, not on the decoded bytes: hexToBytes() drops the last nibble of an odd-length input
    require(ADDRESS_HEX.matches(hex)) { "An EVM address is ${ADDRESS_BYTES * 2} hex characters, got '$this'" }

    return ByteArray(WORD - ADDRESS_BYTES) + hex.hexToBytes()
}

/** The bytes of a `0x`-prefixed hex payload; an odd length is rejected instead of being silently truncated */
internal fun String.hexPayloadToBytes(): ByteArray {
    val hex = removePrefix(prefix = "0x")
    require(hex.length % 2 == 0 && HEX.matches(hex)) { "A hex payload must have an even number of hex characters" }

    return hex.hexToBytes()
}

private val ADDRESS_HEX = Regex(pattern = "^[0-9a-fA-F]{40}$")
private val HEX = Regex(pattern = "^[0-9a-fA-F]*$")