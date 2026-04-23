package com.tangem.domain.swap.models

import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Represents the status of a cryptocurrency in the context of a swap operation.
 *
 * Combines the [UserWallet], [CryptoCurrencyStatus], and [Account] to provide
 * all necessary information about a currency participating in a swap.
 *
 * @property userWallet the user wallet that owns the currency
 * @property status the current status of the cryptocurrency, including balance and value state
 * @property account the account within the wallet that holds the currency
 * @property currency shortcut to the [CryptoCurrency] from [status]
 * @property userWalletId shortcut to the wallet ID from [userWallet]
 * @property isAvailableForSwap whether this currency can participate in a swap operation,
 *   determined by [RampStateManager][com.tangem.domain.exchange.RampStateManager]
 */
data class SwapCurrencyStatus(
    val userWallet: UserWallet,
    val status: CryptoCurrencyStatus,
    val account: Account,
    val isAvailableForSwap: Boolean = true,
) {
    val currency: CryptoCurrency
        get() = status.currency
    val userWalletId: UserWalletId
        get() = userWallet.walletId
}