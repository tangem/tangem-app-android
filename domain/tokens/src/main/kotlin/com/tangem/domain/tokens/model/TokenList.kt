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
        val tokens: NonEmptySet<TokenState>,
        override val totalFiatBalance: FiatBalance,
        override val sortedBy: SortType,
    ) : TokenList()

    object NotInitialized : TokenList()

    enum class SortType {
        NONE, BALANCE,
    }

    sealed class FiatBalance {
        open val amount: BigDecimal? = null

        object Loading : FiatBalance()

        object Failed : FiatBalance()

        data class Loaded(
            override val amount: BigDecimal,
            val isAllAmountsSummarized: Boolean,
        ) : FiatBalance()
    }
}
