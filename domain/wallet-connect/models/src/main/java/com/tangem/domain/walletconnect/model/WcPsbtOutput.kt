package com.tangem.domain.walletconnect.model

/**
 * A single output of a parsed PSBT (Partially Signed Bitcoin Transaction).
 *
 * @property address recipient address decoded from the output script, or `null` if it could not be decoded
 *                   (e.g. `OP_RETURN` or non-standard scripts)
 * @property amountSatoshi output amount, in satoshi
 */
data class WcPsbtOutput(
    val address: String?,
    val amountSatoshi: Long,
)