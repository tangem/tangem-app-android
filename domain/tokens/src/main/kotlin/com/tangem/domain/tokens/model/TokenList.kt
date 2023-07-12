package com.tangem.domain.tokens.model

import arrow.core.NonEmptySet
import java.math.BigDecimal

sealed class TokenList {
    open val totalFiatBalance: FiatBalance = FiatBalance.Loading
    open val sortedBy: SortType = SortType.NONE

    data class GroupedByNetwork(
        val groups: NonEmptySet<NetworkGroup>,
        override val totalFiatBalance: FiatBalance,
        override val sortedBy: SortType,
    ) : TokenList()

    data class Ungrouped(
        val tokens: NonEmptySet<TokenStatus>,
        override val totalFiatBalance: FiatBalance,
        override val sortedBy: SortType,
    ) : TokenList()

    object NotInitialized : TokenList()

    enum class SortType {
        NONE, BALANCE,
    }

    sealed class FiatBalance {
        object Loading : FiatBalance()

        object Failed : FiatBalance()

        data class Loaded(
            val amount: BigDecimal,
            val isAllAmountsSummarized: Boolean,
        ) : FiatBalance()
    }
}
