package com.tangem.domain.models.currency.balance

import com.tangem.domain.models.StatusSource
import java.math.BigDecimal

/**
 * An extra balance that a feature folds into a currency's total, on top of the on-chain network amount.
 *
 * This is the generic ("Axis 1") half of the extra-balance contract: a scalar answer to *"how much do you add
 * to the total?"*. It is intentionally the only thing this base module knows about extra balances — everything
 * rich (a staking breakdown per validator, the yield-supply partition and its APY accrual) is "Axis 2" and
 * stays owned by the feature that produced the contribution, reached by downcasting to the concrete type.
 *
 * Consumers of a total must never branch on the concrete type:
 * ```
 * val total = value.amount + value.contributions.sumOf { it.totalDeltaCryptoAmount() }
 * ```
 *
 * @see com.tangem.domain.models.currency.CryptoCurrencyStatus.Value.contributions
 */
interface BalanceContribution {

    /**
     * Discriminator of the contribution type, e.g. `"staking"`.
     *
     * Declared as a plain [String] on purpose: the constant belongs to the module that owns the balance type,
     * so a new type can be introduced without editing this module. An enum here would recreate exactly the
     * coupling this contract removes.
     */
    val kind: String

    /**
     * Freshness of this contribution; folded generically into
     * [com.tangem.domain.models.currency.CryptoCurrencyStatus.Sources.total].
     */
    val source: StatusSource

    /**
     * Crypto amount that must be **added** to [com.tangem.domain.models.currency.CryptoCurrencyStatus.Value.amount]
     * to obtain the currency total.
     *
     * Extra balances are not uniformly additive, so implementations differ
     */
    fun totalDeltaCryptoAmount(): BigDecimal
}