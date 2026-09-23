package com.tangem.features.txhistory.converter

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.SdkAmountType
import com.tangem.domain.models.network.TxInfo

/**
 * The token a network-fee transaction moved, matched among [networkTokens] by the contract its amount is denominated
 * in — the domain model carries that contract, the avatar lives in the wallet's cached token metadata.
 *
 * `null` for a token the wallet does not track: the row then renders without a name or an avatar.
 */
internal fun TxInfo.TransactionType.NetworkFee.resolveToken(
    networkTokens: Set<CryptoCurrency.Token>,
): CryptoCurrency.Token? {
    val contractAddress = (amount?.type as? SdkAmountType.Token)?.contractAddress ?: return null
    return networkTokens.find { token -> token.contractAddress.equals(contractAddress, ignoreCase = true) }
}