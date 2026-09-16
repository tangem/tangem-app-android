package com.tangem.domain.polymarket.model

import com.tangem.domain.models.currency.CryptoCurrency

/**
 * @property chainId      the bridge's own chain id, sent back verbatim when the withdrawal is submitted
 * @property tokenAddress the bridge's own asset address; the EIP-7528 native sentinel for a coin
 */
data class PredictionWithdrawDestination(
    val currencyId: CryptoCurrency.ID,
    val chainId: String,
    val tokenAddress: String,
)