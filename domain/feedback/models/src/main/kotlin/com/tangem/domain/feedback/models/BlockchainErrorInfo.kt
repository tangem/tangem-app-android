package com.tangem.domain.feedback.models

import com.tangem.domain.models.network.Network

/**
 * Information about blockchain's operation error
 *
 * @property errorMessage       message about error
 * @property networkId          network ID
 * @property destinationAddress destination address
 * @property tokenSymbol        token symbol or null, if it isn't operation with token
 * @property amount             amount
 * @property fee                fee or null, if unable to get
 */
data class BlockchainErrorInfo(
    val errorMessage: String,
    val networkId: Network.ID?,
    val destinationAddress: String,
    val tokenSymbol: String?,
    val amount: String,
    val fee: String?,
)