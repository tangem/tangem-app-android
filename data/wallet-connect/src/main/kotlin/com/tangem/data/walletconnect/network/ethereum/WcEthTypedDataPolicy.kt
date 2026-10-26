package com.tangem.data.walletconnect.network.ethereum

import com.tangem.domain.transaction.usecase.gasless.GaslessTypedDataSchema
import org.json.JSONException
import org.json.JSONObject
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Guards `eth_signTypedData*` against payloads that authorize calls from the user's own account.
 *
 * The EVM gasless flow EIP-7702-delegates the account to `Tangem7702GaslessExecutor`, which executes any
 * `GaslessTransaction`/`GaslessBatchTransaction` signed for `verifyingContract` = the account, regardless of the
 * submitter. A dApp must never obtain such a signature, so these payloads are rejected before signing.
 *
 * Fails closed: anything that cannot be inspected reliably is treated as a violation.
 */
internal object WcEthTypedDataPolicy {

    enum class Violation {
        MALFORMED_PAYLOAD,
        GASLESS_PRIMARY_TYPE,
        GASLESS_EXECUTOR_DOMAIN,
        SIGNER_IS_VERIFYING_CONTRACT,
    }

    private const val KEY_PRIMARY_TYPE = "primaryType"
    private const val KEY_DOMAIN = "domain"
    private const val KEY_NAME = "name"
    private const val KEY_VERIFYING_CONTRACT = "verifyingContract"
    private const val HEX_PREFIX = "0x"
    private const val HEX_RADIX = 16
    private const val ADDRESS_BITS = 160
    private const val MAX_ADDRESS_LITERAL_LENGTH = 80
    private const val MAX_UINT256_DECIMAL_DIGITS = 78

    private val ADDRESS_MODULUS: BigInteger = BigInteger.ONE.shiftLeft(ADDRESS_BITS)

    fun findViolation(typedData: String, signerAddress: String): Violation? {
        val root = try {
            JSONObject(typedData)
        } catch (e: JSONException) {
            return Violation.MALFORMED_PAYLOAD
        }

        if (root.optStringValue(KEY_PRIMARY_TYPE).isAnyOf(GaslessTypedDataSchema.primaryTypes)) {
            return Violation.GASLESS_PRIMARY_TYPE
        }

        val domain = when (val rawDomain = root.opt(KEY_DOMAIN)) {
            null, JSONObject.NULL -> return null
            is JSONObject -> rawDomain
            else -> return Violation.MALFORMED_PAYLOAD
        }

        if (domain.optStringValue(KEY_NAME).isAnyOf(setOf(GaslessTypedDataSchema.DOMAIN_NAME))) {
            return Violation.GASLESS_EXECUTOR_DOMAIN
        }

        return when (val verifyingContract = domain.opt(KEY_VERIFYING_CONTRACT)) {
            null, JSONObject.NULL -> null
            // org.json may round large JSON numbers to Double, so a non-string address cannot be compared reliably
            !is String -> Violation.MALFORMED_PAYLOAD
            else -> when {
                isUnboundedNumber(verifyingContract) -> Violation.MALFORMED_PAYLOAD
                isSameAddress(verifyingContract, signerAddress) -> Violation.SIGNER_IS_VERIFYING_CONTRACT
                else -> null
            }
        }
    }

    private fun JSONObject.optStringValue(key: String): String? = opt(key) as? String

    private fun String?.isAnyOf(forbidden: Set<String>): Boolean {
        val value = this?.trim() ?: return false
        return forbidden.any { it.equals(value, ignoreCase = true) }
    }

    private fun isSameAddress(verifyingContract: String, signerAddress: String): Boolean {
        if (verifyingContract.trim().equals(signerAddress.trim(), ignoreCase = true)) return true
        val contractValue = parseTypedDataAddress(verifyingContract) ?: return false
        val signerValue = parseHexAddress(signerAddress) ?: return false
        return contractValue == signerValue
    }

    /**
     * A short literal such as `1e100000000` or `1e-100000000` makes `BigDecimal.toBigIntegerExact()` (ours and the
     * SDK encoder's) materialize a huge power of ten, so values that cannot be a 256-bit integer are refused upfront.
     */
    private fun isUnboundedNumber(raw: String): Boolean {
        val value = raw.trim()
        if (value.length > MAX_ADDRESS_LITERAL_LENGTH) return true
        if (value.startsWith(HEX_PREFIX, ignoreCase = true)) return false
        val number = value.toBigDecimalOrNull() ?: return false
        return try {
            val normalized = number.stripTrailingZeros()
            val integerDigits = normalized.precision().toLong() - normalized.scale()
            normalized.scale() > 0 || integerDigits > MAX_UINT256_DECIMAL_DIGITS
        } catch (e: ArithmeticException) {
            // stripTrailingZeros() overflows the Int scale for literals such as `100e2147483647`
            true
        }
    }

    /**
     * Mirrors how the Blockchain SDK's EIP-712 encoder reads an `address` value: `0x`-prefixed hex, otherwise
     * a decimal number. Comparing numerically keeps padded, prefix-less or decimal spellings from slipping through.
     */
    private fun parseTypedDataAddress(raw: String): BigInteger? {
        val value = raw.trim()
        val number = try {
            if (value.startsWith(HEX_PREFIX, ignoreCase = true)) {
                BigInteger(value.drop(HEX_PREFIX.length), HEX_RADIX)
            } else {
                BigDecimal(value).toBigIntegerExact()
            }
        } catch (e: NumberFormatException) {
            return null
        } catch (e: ArithmeticException) {
            return null
        }
        return number.mod(ADDRESS_MODULUS)
    }

    private fun parseHexAddress(raw: String): BigInteger? {
        val value = raw.trim().takeIf { it.length <= MAX_ADDRESS_LITERAL_LENGTH } ?: return null
        val digits = if (value.startsWith(HEX_PREFIX, ignoreCase = true)) value.drop(HEX_PREFIX.length) else value
        return digits.toBigIntegerOrNull(HEX_RADIX)?.mod(ADDRESS_MODULUS)
    }
}