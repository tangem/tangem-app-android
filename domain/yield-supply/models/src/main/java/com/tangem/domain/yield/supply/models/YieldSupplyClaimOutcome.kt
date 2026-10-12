package com.tangem.domain.yield.supply.models

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

/**
 * How a submitted Merkl rewards claim ended, once the chain could answer for it.
 *
 * @property chainId module the claim was made on; one claim settles the rewards of every token sharing it
 * @property cryptoCurrencyId token the claim was started from, which is what the screens that started it are keyed by
 */
data class YieldSupplyClaimOutcome(
    val userWalletId: UserWalletId,
    val cryptoCurrencyId: CryptoCurrency.ID,
    val chainId: Int,
    val userAddress: String,
    val outcome: YieldSupplyTransactionOutcome,
)