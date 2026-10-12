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
    val isYieldSupplyActive: Boolean
        get() = status.value.yieldSupplyStatus?.isActive == true

    /** @param isPredictionWithdraw whether the swap screen was opened to withdraw from the prediction account */
    fun isTargetOnly(isPredictionWithdraw: Boolean): Boolean = account.isSwapTargetOnly(isPredictionWithdraw)
}

/**
 * Whether the account can receive a swap but never fund one: it holds its currency at an address of an
 * integration's own, so the wallet has no key that could spend it. A prediction withdrawal is the exception —
 * there the debit is not signed with a wallet key but submitted as a signed batch through our backend, so the
 * account is the source.
 *
 * @param isPredictionWithdraw whether the swap screen was opened to withdraw from the prediction account
 */
fun Account.isSwapTargetOnly(isPredictionWithdraw: Boolean): Boolean =
    this is Account.Joint || this is Account.Prediction && !isPredictionWithdraw