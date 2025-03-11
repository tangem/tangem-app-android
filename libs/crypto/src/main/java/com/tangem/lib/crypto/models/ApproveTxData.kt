package com.tangem.lib.crypto.models

import java.math.BigDecimal

/**
 * Tx data for create and make approve transaction
 *
 * @property networkId id of network
 * @property feeAmount amount of fee
 * @property gasLimit gasLimit for given tx
 * @property destinationAddress address to send tx
 * @property dataToSign data to sing with signer
 */
data class ApproveTxData(
    val networkId: String,
    val feeAmount: BigDecimal,
    val gasLimit: Int,
    val destinationAddress: String,
    val dataToSign: String,
)