package com.tangem.utils.extensions

private const val BYTES_IN_HEX = 2
private const val RADIX = 16

/**
 * Converts a [ByteArray] to its hexadecimal string representation
 *
 * @return a [String] containing the hexadecimal representation of the byte array
 */
fun ByteArray.toHexString(): String = joinToString("") { "%02X".format(it) }

/**
 * Converts a hexadecimal [String] to a [ByteArray]
 *
 * @return a [ByteArray] representing the binary data of the hexadecimal string
 * @throws NumberFormatException if the string is not a valid hexadecimal representation
 */
fun String.hexToBytes(): ByteArray {
    val hexString = this.removePrefix("0x")

    return ByteArray(size = hexString.length / BYTES_IN_HEX) { i ->
        Integer.parseInt(
            hexString.substring(
                startIndex = BYTES_IN_HEX * i,
                endIndex = BYTES_IN_HEX * i + BYTES_IN_HEX,
            ),
            RADIX,
        ).toByte()
    }
}

/**
 * Safely converts a hexadecimal [String] to a [ByteArray]
 *
 * @return a [ByteArray] if the conversion is successful, or `null` if an exception occurs
 */
fun String.hexToBytesOrNull(): ByteArray? = runCatching(String::hexToBytes).getOrNull()