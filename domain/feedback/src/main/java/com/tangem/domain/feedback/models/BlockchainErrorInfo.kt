package com.tangem.domain.feedback.models

/**
 * Information about blockchain's operation error
 *
 * @property errorMessage       message about error
 * @property blockchainId       blockchain id
 * @property derivationPath     derivation path
 * @property destinationAddress destination address
 * @property tokenSymbol        token symbol or null, if it isn't operation with token
 * @property amount             amount
 * @property fee                fee or null, if unable to get
 */
data class BlockchainErrorInfo(
    val errorMessage: String,
    val blockchainId: String,
    val derivationPath: String?,
    val destinationAddress: String,
    val tokenSymbol: String?,
    val amount: String,
    val fee: String?,
)