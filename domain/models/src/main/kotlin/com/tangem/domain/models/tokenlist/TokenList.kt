package com.tangem.domain.models.tokenlist

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable

/**
 * Represents a list of cryptocurrency tokens, which can be grouped by network or ungrouped.
 *
 * The tokens can be represented in two forms: either grouped by the network or as an ungrouped collection.
 * Additional details like the total fiat balance and the sorting type can be associated with the list.
 */
@Serializable
sealed interface TokenList {

    /** The total fiat balance across all tokens */
    val totalFiatBalance: TotalFiatBalance

    /** The criteria used for sorting the tokens */
    val sortedBy: TokensSortType

    /**
     * Represents tokens that are grouped by their network
     *
     * @property totalFiatBalance the total fiat balance across all groups
     * @property sortedBy         the criteria used for sorting the tokens within the groups
     * @property groups           a list of network groups containing tokens
     */
    @Serializable
    data class GroupedByNetwork(
        override val totalFiatBalance: TotalFiatBalance,
        override val sortedBy: TokensSortType,
        val groups: List<NetworkGroup>,
    ) : TokenList {

        /**
         * Represents a group of cryptocurrencies associated with a specific network
         *
         * @property network    the blockchain network associated with the group
         * @property currencies a list of cryptocurrency statuses that belong to the network
         */
        @Serializable
        data class NetworkGroup(
            val network: Network,
            val currencies: List<CryptoCurrencyStatus>,
        )
    }

    /**
     * Represents tokens that are not grouped by any specific criteria.
     *
     * @property totalFiatBalance the total fiat balance across all groups
     * @property sortedBy         the criteria used for sorting the tokens within the groups
     * @property currencies       a list of cryptocurrency statuses
     */
    @Serializable
    data class Ungrouped(
        override val totalFiatBalance: TotalFiatBalance,
        override val sortedBy: TokensSortType,
        val currencies: List<CryptoCurrencyStatus>,
    ) : TokenList

    /** Represents a state where the token list is empty */
    @Serializable
    data object Empty : TokenList {

        override val totalFiatBalance: TotalFiatBalance = TotalFiatBalance.Loaded(
            amount = SerializedBigDecimal.ZERO,
            isAllAmountsSummarized = true,
            source = StatusSource.ACTUAL,
        )

        override val sortedBy: TokensSortType = TokensSortType.NONE
    }

    /** Get flatten list of cryptocurrency status [CryptoCurrencyStatus] */
    fun flattenCurrencies(): List<CryptoCurrencyStatus> {
        return when (this) {
            is GroupedByNetwork -> groups.flatMap(GroupedByNetwork.NetworkGroup::currencies)
            is Ungrouped -> currencies
            is Empty -> emptyList()
        }
    }
}