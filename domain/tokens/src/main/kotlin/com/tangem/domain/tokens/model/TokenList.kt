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
    open val totalFiatBalance: FiatBalance = FiatBalance.Loading
    open val sortedBy: SortType = SortType.NONE

    /**
     * Represents tokens that are grouped by their network.
     *
     * @property groups A set of network groups containing tokens.
     * @property totalFiatBalance The total fiat balance across all groups.
     * @property sortedBy The criteria used for sorting the tokens within the groups.
     */
    data class GroupedByNetwork(
        val groups: Set<NetworkGroup>,
        override val totalFiatBalance: FiatBalance,
        override val sortedBy: SortType,
    ) : TokenList()

    /**
     * Represents tokens that are not grouped by any specific criteria.
     *
     * @property currencies A set of cryptocurrency statuses.
     * @property totalFiatBalance The total fiat balance across all currencies.
     * @property sortedBy The criteria used for sorting the currencies.
     */
    data class Ungrouped(
        val currencies: Set<CryptoCurrencyStatus>,
        override val totalFiatBalance: FiatBalance,
        override val sortedBy: SortType,
    ) : TokenList()

    /** Represents a state where the token list is not initialized. */
    object NotInitialized : TokenList()

    /** Defines the possible sorting criteria for the tokens. */
    enum class SortType {
        NONE, BALANCE,
    }

    /**
     * Represents the possible states of the fiat balance, including loading, failure, or a loaded amount.
     */
    sealed class FiatBalance {
        /**
         * Represents the loading state of the fiat balance.
         * This state indicates that the fiat balance is currently being retrieved or calculated.
         */
        object Loading : FiatBalance()

        /**
         * Represents the failure state of the fiat balance.
         * This state indicates that an attempt to retrieve or calculate the fiat balance has failed.
         */
        object Failed : FiatBalance()

        /**
         * Represents the successfully loaded state of the fiat balance.
         *
         * @property amount The loaded fiat balance amount.
         * @property isAllAmountsSummarized Indicates whether the amount includes a summary of all underlying amounts.
         */
        data class Loaded(
            val amount: BigDecimal,
            val isAllAmountsSummarized: Boolean,
        ) : FiatBalance()
    }
}
