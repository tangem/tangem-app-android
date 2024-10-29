package com.tangem.domain.tokens.model

import java.math.BigDecimal

/**
 * Represents a list of cryptocurrency tokens, which can be grouped by network or ungrouped.
 *
 * The tokens can be represented in two forms: either grouped by the network or as an ungrouped collection.
 * Additional details like the total fiat balance and the sorting type can be associated with the list.
 *
 * @property totalFiatBalance The total fiat balance across all tokens, which could be in a loading state, failed, or loaded with a specific amount.
 * @property sortedBy The criteria used for sorting the tokens.
 */
sealed class TokenList {
    open val totalFiatBalance: TotalFiatBalance = TotalFiatBalance.Loading
    open val sortedBy: SortType = SortType.NONE

    /**
     * Represents tokens that are grouped by their network.
     *
     * @property groups A list of network groups containing tokens.
     * @property totalFiatBalance The total fiat balance across all groups.
     * @property sortedBy The criteria used for sorting the tokens within the groups.
     */
    data class GroupedByNetwork(
        val groups: List<NetworkGroup>,
        override val totalFiatBalance: TotalFiatBalance,
        override val sortedBy: SortType,
    ) : TokenList()

    /**
     * Represents tokens that are not grouped by any specific criteria.
     *
     * @property currencies A list of cryptocurrency statuses.
     * @property totalFiatBalance The total fiat balance across all currencies.
     * @property sortedBy The criteria used for sorting the currencies.
     */
    data class Ungrouped(
        val currencies: List<CryptoCurrencyStatus>,
        override val totalFiatBalance: TotalFiatBalance,
        override val sortedBy: SortType,
    ) : TokenList()

    /** Represents a state where the token list is empty. */
    data object Empty : TokenList() {

        override val totalFiatBalance: TotalFiatBalance = TotalFiatBalance.Loaded(
            amount = BigDecimal.ZERO,
            isAllAmountsSummarized = true,
        )
    }

    /** Defines the possible sorting criteria for the tokens. */
    enum class SortType {
        NONE, BALANCE,
    }

    /** Get flatten list of cryptocurrency status [CryptoCurrencyStatus] */
    fun flattenCurrencies(): List<CryptoCurrencyStatus> {
        return when (this) {
            is GroupedByNetwork -> groups.flatMap(NetworkGroup::currencies)
            is Ungrouped -> currencies
            is Empty -> emptyList()
        }
    }
}
