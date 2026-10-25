package com.tangem.domain.polymarket.sessionkey

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.domain.polymarket.model.PolymarketApprovalCall
import com.tangem.domain.polymarket.model.PolymarketSessionKeyError

object PolymarketSessionKeyCalls {

    private const val AUTHORIZE_SELECTOR = "0x24017fae"
    private const val REVOKE_SELECTOR = "0xe63f952f"
    private const val HEX_PREFIX = "0x"
    private const val ADDRESS_HEX_LENGTH = 40
    private const val WORD_HEX_LENGTH = 64
    private const val HEX_RADIX = 16
    private const val MILLIS_IN_SECOND = 1_000L
    private const val VALIDITY_SECONDS = 180L * 24 * 60 * 60

    /**
     * @return the unix-seconds expiry of a key authorized at [nowMillis], 180 days from its whole second
     */
    fun validUntil(nowMillis: Long): Long = nowMillis / MILLIS_IN_SECOND + VALIDITY_SECONDS

    /**
     * @param validUntil unix seconds, the value sent to the BFF alongside the batch
     * @return the `authorizeSessionSigner(address,uint256)` call to [depositWalletAddress]
     */
    fun authorize(
        sessionKeyAddress: String,
        validUntil: Long,
        depositWalletAddress: String,
    ): Either<PolymarketSessionKeyError.InvalidAddress, PolymarketApprovalCall> = either {
        call(
            depositWalletAddress = depositWalletAddress,
            data = AUTHORIZE_SELECTOR + addressWord(sessionKeyAddress) + uintWord(validUntil),
        )
    }

    /** @return the `revokeSessionSigner(address)` call to [depositWalletAddress] */
    fun revoke(
        sessionKeyAddress: String,
        depositWalletAddress: String,
    ): Either<PolymarketSessionKeyError.InvalidAddress, PolymarketApprovalCall> = either {
        call(depositWalletAddress = depositWalletAddress, data = REVOKE_SELECTOR + addressWord(sessionKeyAddress))
    }

    private fun Raise<PolymarketSessionKeyError.InvalidAddress>.call(
        depositWalletAddress: String,
        data: String,
    ): PolymarketApprovalCall = PolymarketApprovalCall(
        target = HEX_PREFIX + addressDigits(depositWalletAddress),
        value = "0",
        data = data,
    )

    private fun Raise<PolymarketSessionKeyError.InvalidAddress>.addressWord(address: String): String =
        addressDigits(address).padStart(WORD_HEX_LENGTH, '0')

    private fun Raise<PolymarketSessionKeyError.InvalidAddress>.addressDigits(address: String): String {
        val digits = address.removePrefix(HEX_PREFIX)
        ensure(digits.length == ADDRESS_HEX_LENGTH && digits.all { it.isHexDigit() }) {
            PolymarketSessionKeyError.InvalidAddress(address)
        }
        return digits.lowercase()
    }

    private fun uintWord(value: Long): String = value.toString(HEX_RADIX).padStart(WORD_HEX_LENGTH, '0')

    private fun Char.isHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
}