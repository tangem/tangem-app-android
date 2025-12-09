package com.tangem.domain.tokens.operations

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.tokenlist.TokenList.GroupedByNetwork.NetworkGroup
import com.tangem.domain.staking.utils.getTotalWithRewardsStakingBalance
import com.tangem.utils.extensions.orZero
import java.math.BigDecimal

/**
 * This factory creates a [TokenList] based on the provided list of [CryptoCurrencyStatus], [TokensGroupType],
 * and [TokensSortType].
 *
[REDACTED_AUTHOR]
 */
object TokenListFactory {

    /**
     * Creates a [TokenList] based on the provided parameters.
     *
     * @param statuses A list of [CryptoCurrencyStatus] representing the cryptocurrencies to be included in the token list.
     * @param groupType The type of grouping to be applied to the token list, either by network or none.
     * @param sortType The type of sorting to be applied to the token list, either by balance or none.
     */
    fun create(statuses: List<CryptoCurrencyStatus>, groupType: TokensGroupType, sortType: TokensSortType): TokenList {
        val nonEmptyStatuses = statuses.toNonEmptyListOrNull() ?: return TokenList.Empty
        val totalFiatBalance = TotalFiatBalanceCalculator.calculate(statuses = nonEmptyStatuses)

        return when (groupType) {
            TokensGroupType.NONE -> createUngrouped(totalFiatBalance, nonEmptyStatuses, sortType)
            TokensGroupType.NETWORK -> createGroupedByNetwork(totalFiatBalance, nonEmptyStatuses, sortType)
        }
    }

    fun createUngrouped(tokenList: TokenList.GroupedByNetwork): TokenList.Ungrouped {
        return createUngrouped(
            totalFiatBalance = tokenList.totalFiatBalance,
            nonEmptyStatuses = tokenList.flattenCurrencies().toNonEmptyListOrNull()
                ?: error("GroupedByNetwork.flattenCurrencies should be non empty list"),
            sortType = tokenList.sortedBy,
        )
    }

    fun createGroupedByNetwork(tokenList: TokenList.Ungrouped): TokenList.GroupedByNetwork {
        return createGroupedByNetwork(
            totalFiatBalance = tokenList.totalFiatBalance,
            nonEmptyStatuses = tokenList.flattenCurrencies().toNonEmptyListOrNull()
                ?: error("Ungrouped.flattenCurrencies should be non empty list"),
            sortType = tokenList.sortedBy,
        )
    }

    private fun createUngrouped(
        totalFiatBalance: TotalFiatBalance,
        nonEmptyStatuses: NonEmptyList<CryptoCurrencyStatus>,
        sortType: TokensSortType,
    ): TokenList.Ungrouped {
        return TokenList.Ungrouped(
            totalFiatBalance = totalFiatBalance,
            sortedBy = sortType,
            currencies = nonEmptyStatuses.sortStatuses(sortType),
        )
    }

    private fun createGroupedByNetwork(
        totalFiatBalance: TotalFiatBalance,
        nonEmptyStatuses: NonEmptyList<CryptoCurrencyStatus>,
        sortType: TokensSortType,
    ): TokenList.GroupedByNetwork {
        return TokenList.GroupedByNetwork(
            totalFiatBalance = totalFiatBalance,
            sortedBy = sortType,
            groups = nonEmptyStatuses.groupByNetworkAndSort(sortType),
        )
    }

    private fun NonEmptyList<CryptoCurrencyStatus>.groupByNetworkAndSort(sortType: TokensSortType): List<NetworkGroup> {
        return this
            .groupBy { it.currency.network }
            .map { (network, currencies) ->
                NetworkGroup(
                    network = network,
                    currencies = currencies.sortStatuses(sortType),
                )
            }
            .sortGroups(sortType)
    }

    private fun List<NetworkGroup>.sortGroups(type: TokensSortType): List<NetworkGroup> {
        return when (type) {
            TokensSortType.NONE -> this
            TokensSortType.BALANCE -> {
                val hasLoading = this.asSequence()
                    .flatMap(NetworkGroup::currencies)
                    .any { it.value is CryptoCurrencyStatus.Loading }

                if (hasLoading) return this

                sortedByDescending { group ->
                    group.currencies.sumOf { it.calculateBalance() }
                }
            }
        }
    }

    private fun List<CryptoCurrencyStatus>.sortStatuses(type: TokensSortType): List<CryptoCurrencyStatus> {
        return when (type) {
            TokensSortType.NONE -> this
            TokensSortType.BALANCE -> {
                val hasLoading = any { it.value is CryptoCurrencyStatus.Loading }

                if (hasLoading) return this

                sortedByDescending { it.calculateBalance() }
            }
        }
    }

    private fun CryptoCurrencyStatus.calculateBalance(): BigDecimal {
        val stakingBalance = value.stakingBalance as? StakingBalance.Data
        val totalStakingBalance = stakingBalance?.getTotalWithRewardsStakingBalance(currency.network.rawId).orZero()
        val totalFiatStakingBalance = totalStakingBalance.multiply(value.fiatRate.orZero())

        return value.fiatAmount?.plus(totalFiatStakingBalance).orZero()
    }
}