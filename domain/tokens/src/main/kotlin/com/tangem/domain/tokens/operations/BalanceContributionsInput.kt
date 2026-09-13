package com.tangem.domain.tokens.operations

import com.tangem.domain.models.currency.balance.BalanceContribution

/**
 * What the status seam hands [CryptoCurrencyStatusFactory]: the extra balances its
 * `BalanceContributionProvider`s produced for one currency, and whether that path is on at all.
 *
 * [Disabled] keeps the factory on the legacy typed-field path — the two are mutually exclusive, and the switch
 * between them is the balance-contributions migration gate (`TWI_1717_BALANCE_CONTRIBUTIONS`). When the toggle
 * goes away only [enabled] remains and this type collapses into a plain list.
 *
 * @property contributions extra balances for the currency; may be empty even when [isEnabled].
 * @property isEnabled whether [contributions] should be used instead of the legacy staking field.
 */
data class BalanceContributionsInput(
    val contributions: List<BalanceContribution>,
    val isEnabled: Boolean,
) {

    companion object {

        /** Legacy path: extra balances are read from the typed fields of `CryptoCurrencyStatus.Value`. */
        val Disabled = BalanceContributionsInput(contributions = emptyList(), isEnabled = false)

        /** Contributions path: [contributions] is the only source of extra balances for the currency. */
        fun enabled(contributions: List<BalanceContribution>) = BalanceContributionsInput(
            contributions = contributions,
            isEnabled = true,
        )
    }
}