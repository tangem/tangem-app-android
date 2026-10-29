package com.tangem.domain.polymarket.sessionkey

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure

object PolymarketSessionKeyEnvelope {

    private const val HEX_PREFIX = "0x"
    private const val WORD_HEX_LENGTH = 64
    private const val ADDRESS_HEX_LENGTH = 40
    private const val HEX_CHARS_PER_BYTE = 2
    private const val WORD_SIZE = 32
    private const val SIGNER_ID_WORD = 0
    private const val RESERVED_WORD = 1
    private const val OFFSET_WORD = 2
    private const val LENGTH_WORD = 3
    private const val HEAD_WORDS = 4
    private const val HEX_RADIX = 16
    private const val PAYLOAD_OFFSET = 3 * WORD_SIZE

    private val MAGIC_SUFFIX = "6492".repeat(n = 16)
    private val ZERO_WORD = "0".repeat(n = WORD_HEX_LENGTH)

    /**
     * @param depositWalletSignature the ERC-7739-wrapped signature of the order, `0x`-prefixed hex
     * @param sessionKeyAddress the address of the key that produced the inner signature
     * @return the `0x`-prefixed lower-case envelope
     */
    fun wrap(depositWalletSignature: String, sessionKeyAddress: String): Either<Error, String> = either {
        val address = sessionKeyAddress.removePrefix(HEX_PREFIX).lowercase()
        ensure(address.length == ADDRESS_HEX_LENGTH && address.isHex()) { Error.InvalidAddress(sessionKeyAddress) }

        val signature = depositWalletSignature.removePrefix(HEX_PREFIX).lowercase()
        ensure(signature.isNotEmpty() && signature.length % HEX_CHARS_PER_BYTE == 0 && signature.isHex()) {
            Error.InvalidSignature
        }

        HEX_PREFIX +
            address.padStart(WORD_HEX_LENGTH, '0') +
            ZERO_WORD +
            uintWord(PAYLOAD_OFFSET) +
            uintWord(signature.length / HEX_CHARS_PER_BYTE) +
            signature.padEnd(paddedLength(signature.length), '0') +
            MAGIC_SUFFIX
    }

    /** @return the session key and the deposit-wallet signature [signature] carries, `null` if it is no envelope */
    fun unwrap(signature: String): Unwrapped? {
        val hex = signature.removePrefix(HEX_PREFIX).lowercase()
        if (!hex.endsWith(MAGIC_SUFFIX) || !hex.isHex()) return null

        val payload = hex.dropLast(MAGIC_SUFFIX.length)
        if (payload.length < HEAD_WORDS * WORD_HEX_LENGTH) return null

        val signerId = payload.word(SIGNER_ID_WORD)
        val isWellFormedHead = signerId.startsWith("0".repeat(n = WORD_HEX_LENGTH - ADDRESS_HEX_LENGTH)) &&
            payload.word(RESERVED_WORD) == ZERO_WORD &&
            payload.word(OFFSET_WORD) == uintWord(PAYLOAD_OFFSET)
        val innerLength = payload.word(LENGTH_WORD).toBigInteger(radix = HEX_RADIX)
        val tail = payload.drop(HEAD_WORDS * WORD_HEX_LENGTH)
        val maxInnerLength = (tail.length / HEX_CHARS_PER_BYTE).toBigInteger()
        if (!isWellFormedHead || innerLength.signum() == 0 || innerLength > maxInnerLength) return null

        val innerHexLength = innerLength.toInt() * HEX_CHARS_PER_BYTE
        if (tail.length != paddedLength(innerHexLength)) return null

        val inner = tail.take(innerHexLength)
        if (tail.drop(innerHexLength).any { it != '0' }) return null

        return Unwrapped(
            sessionKeyAddress = HEX_PREFIX + signerId.takeLast(ADDRESS_HEX_LENGTH),
            depositWalletSignature = HEX_PREFIX + inner,
        )
    }

    private fun String.word(index: Int): String = substring(index * WORD_HEX_LENGTH, (index + 1) * WORD_HEX_LENGTH)

    private fun uintWord(value: Int): String = value.toString(HEX_RADIX).padStart(WORD_HEX_LENGTH, '0')

    private fun paddedLength(hexLength: Int): Int =
        (hexLength + WORD_HEX_LENGTH - 1) / WORD_HEX_LENGTH * WORD_HEX_LENGTH

    private fun String.isHex(): Boolean = all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }

    /**
     * @property sessionKeyAddress lower-case `0x`-prefixed address of the signing key
     * @property depositWalletSignature the ERC-7739-wrapped signature, `0x`-prefixed
     */
    data class Unwrapped(val sessionKeyAddress: String, val depositWalletSignature: String)

    sealed interface Error {

        data class InvalidAddress(val address: String) : Error

        data object InvalidSignature : Error
    }
}