package com.tangem.features.txhistory.converter

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.SdkAmount
import com.tangem.domain.models.network.SdkAmountType

/**
 * The token this amount is denominated in, matched among [networkTokens] by the contract the domain model carries —
 * the avatar and the name live in the wallet's cached token metadata, not in the transaction.
 *
 * `null` for a coin amount and for a token the wallet does not track: the caller then falls back to the viewed
 * currency, or renders without a name and an avatar.
 */
internal fun SdkAmount?.resolveToken(networkTokens: Set<CryptoCurrency.Token>): CryptoCurrency.Token? {
    val contractAddress = (this?.type as? SdkAmountType.Token)?.contractAddress ?: return null
    return networkTokens.find { token -> token.contractAddress.equals(contractAddress, ignoreCase = true) }
}