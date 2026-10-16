package com.tangem.domain.yield.supply.models

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

/**
 * @property chainId with [userAddress] identifies the module; one claim settles the rewards of every token of it
 * @property cryptoCurrencyId token the claim was started from
 */
data class YieldSupplyClaimOutcome(
    val userWalletId: UserWalletId,
    val cryptoCurrencyId: CryptoCurrency.ID,
    val chainId: Int,
    val userAddress: String,
    val outcome: YieldSupplyTransactionOutcome,
)