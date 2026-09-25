package com.tangem.domain.jointaccount.safe

import com.tangem.domain.jointaccount.model.SafeExecution
import com.tangem.domain.jointaccount.model.SafeInnerCall
import com.tangem.domain.jointaccount.model.SafeRefund
import java.math.BigInteger

/**
 * Reads a Safe `execTransaction` out of transaction calldata: a direct call, or the first one inside the Multicall3
 * `aggregate3` bundle the backend sends when the Safe is deployed and used in one transaction. Never throws.
 */
object SafeExecTransactionDecoder {

    const val EXEC_TRANSACTION_SELECTOR = "6a761202"
    const val AGGREGATE3_SELECTOR = "82ad56cb"

    private const val HEX_PREFIX = "0x"
    private const val HEX_RADIX = 16
    private const val HEX_CHARS_PER_BYTE = 2
    private const val SELECTOR_HEX_LENGTH = 8
    private const val WORD_HEX_LENGTH = 64
    private const val ADDRESS_HEX_LENGTH = 40
    private const val SIGNATURE_BYTES = 65
    private const val TUPLE_CALLDATA_WORD = 2
    private val WORD_BYTES: BigInteger = BigInteger.valueOf(32)

    fun decode(callData: String): SafeExecution? {
        val hex = callData.removePrefix(HEX_PREFIX).lowercase()
        if (hex.length % HEX_CHARS_PER_BYTE != 0 || !hex.all(::isHexDigit)) return null

        val arguments = AbiArguments(hex.drop(SELECTOR_HEX_LENGTH))
        return when (hex.take(SELECTOR_HEX_LENGTH)) {
            EXEC_TRANSACTION_SELECTOR -> decodeExecTransaction(arguments)
            AGGREGATE3_SELECTOR -> decodeAggregate3(arguments)
            else -> null
        }
    }

    /** `execTransaction(to, value, data, operation, safeTxGas, baseGas, gasPrice, gasToken, refundReceiver, signatures)` */
    private fun decodeExecTransaction(arguments: AbiArguments): SafeExecution? = runCatching {
        val signatures = arguments.bytesAt(offset = arguments.uintAt(word = 9))
        SafeExecution(
            call = SafeInnerCall(
                to = arguments.addressAt(word = 0),
                value = arguments.uintAt(word = 1),
                data = HEX_PREFIX + arguments.bytesAt(offset = arguments.uintAt(word = 2)),
            ),
            operation = arguments.uintAt(word = 3).toIntExact(),
            refund = SafeRefund(
                safeTxGas = arguments.uintAt(word = 4),
                baseGas = arguments.uintAt(word = 5),
                gasPrice = arguments.uintAt(word = 6),
                gasToken = arguments.addressAt(word = 7),
                refundReceiver = arguments.addressAt(word = 8),
            ),
            signaturesCount = signatures.length / HEX_CHARS_PER_BYTE / SIGNATURE_BYTES,
        )
    }.getOrNull()

    /** `aggregate3((address target, bool allowFailure, bytes callData)[] calls)` */
    private fun decodeAggregate3(arguments: AbiArguments): SafeExecution? = runCatching {
        val arrayOffset = arguments.uintAt(word = 0)
        val callsCount = arguments.uintAt(offset = arrayOffset).toIntExact()
        val elementsStart = arrayOffset + WORD_BYTES
        (0 until callsCount).firstNotNullOfOrNull { index ->
            val tupleOffset = elementsStart + arguments.uintAt(offset = elementsStart + WORD_BYTES.times(index))
            val callDataWord = tupleOffset + WORD_BYTES.times(TUPLE_CALLDATA_WORD)
            val callDataOffset = tupleOffset + arguments.uintAt(offset = callDataWord)
            arguments.bytesAt(offset = callDataOffset)
                .takeIf { callData -> callData.startsWith(EXEC_TRANSACTION_SELECTOR) }
                ?.let { callData -> decodeExecTransaction(AbiArguments(callData.drop(SELECTOR_HEX_LENGTH))) }
        }
    }.getOrNull()

    private fun isHexDigit(char: Char): Boolean = char in '0'..'9' || char in 'a'..'f'

    private fun BigInteger.times(count: Int): BigInteger = multiply(BigInteger.valueOf(count.toLong()))

    /** `BigInteger.intValueExact` needs API 31 */
    private fun BigInteger.toIntExact(): Int {
        if (signum() < 0 || bitLength() >= Int.SIZE_BITS) throw ArithmeticException("Value out of Int range")
        return toInt()
    }

    /** Offsets are in bytes from the start of the argument block, as the ABI encodes them */
    private class AbiArguments(private val hex: String) {

        fun uintAt(word: Int): BigInteger = uintAt(offset = WORD_BYTES.times(word))

        fun uintAt(offset: BigInteger): BigInteger = BigInteger(wordAt(offset), HEX_RADIX)

        fun addressAt(word: Int): String = HEX_PREFIX + wordAt(WORD_BYTES.times(word)).takeLast(ADDRESS_HEX_LENGTH)

        fun bytesAt(offset: BigInteger): String {
            val length = uintAt(offset).toIntExact() * HEX_CHARS_PER_BYTE
            val start = (offset + WORD_BYTES).toIntExact() * HEX_CHARS_PER_BYTE
            return hex.substring(start, start + length)
        }

        private fun wordAt(offset: BigInteger): String {
            val start = offset.toIntExact() * HEX_CHARS_PER_BYTE
            return hex.substring(start, start + WORD_HEX_LENGTH)
        }
    }
}