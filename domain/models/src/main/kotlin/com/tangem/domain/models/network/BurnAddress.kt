package com.tangem.domain.models.network

/** Addresses nobody holds the private key for: anything sent there is destroyed with no way to recover it. */
private val BURN_ADDRESS_BODIES = setOf(
    // 0x0000000000000000000000000000000000000000
    "0000000000000000000000000000000000000000",
    // 0x000000000000000000000000000000000000dEaD
    "000000000000000000000000000000000000dead",
)

private const val HEX_PREFIX = "0x"

/**
 * Whether this address is a burn address, see [BURN_ADDRESS_BODIES]. Such an address is well-formed, so nothing
 * else in the validation chain rejects it — it has to be blacklisted explicitly.
 *
 * Case- and prefix-insensitive: checksummed addresses mix the case, and a recipient decoded back from call data
 * comes without the `0x` prefix.
 */
fun String.isBurnAddress(): Boolean {
    val body = trim().removePrefix(HEX_PREFIX).removePrefix(HEX_PREFIX.uppercase()).lowercase()

    return body in BURN_ADDRESS_BODIES
}