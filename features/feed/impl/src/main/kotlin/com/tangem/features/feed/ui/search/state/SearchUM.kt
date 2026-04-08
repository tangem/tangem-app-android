package com.tangem.features.feed.ui.search.state

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.account.AccountNameUM
import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.domain.models.account.CryptoPortfolioIcon
import kotlinx.collections.immutable.ImmutableList

data class SearchUM(
    val searchBar: SearchBarUM,
    val content: SearchContentUM,
)

@Immutable
sealed interface SearchContentUM {

    data class History(
        val textHints: ImmutableList<TextHintItemUM>,
        val recentTokens: ImmutableList<MarketsListItemUM>,
    ) : SearchContentUM

    data class Results(
        val userAssets: ImmutableList<UserAssetItemUM>,
        val marketTokens: MarketSearchResultUM,
    ) : SearchContentUM

    data object InitialEmpty : SearchContentUM
}

@Immutable
sealed interface MarketSearchResultUM {

    data class Content(
        val items: ImmutableList<MarketsListItemUM>,
        val shouldShowUnder100kNotification: Boolean = false,
        val onShowUnder100kClick: () -> Unit = {},
    ) : MarketSearchResultUM

    data object Loading : MarketSearchResultUM
    data object NotFound : MarketSearchResultUM
    data object Empty : MarketSearchResultUM
}

data class TextHintItemUM(val text: String)

@Immutable
sealed interface UserAssetItemUM {
    val id: String
    val icon: TangemIconUM
    val tokenName: String
    val tokenSymbol: String
    val onClick: () -> Unit

    data class Single(
        override val id: String,
        override val icon: TangemIconUM,
        override val tokenName: String,
        override val tokenSymbol: String,
        val fiatRate: String?,
        val cryptoBalance: String,
        val fiatBalance: String,
        val isBalanceHidden: Boolean,
        override val onClick: () -> Unit,
    ) : UserAssetItemUM

    data class Grouped(
        override val id: String,
        override val icon: TangemIconUM,
        override val tokenName: String,
        override val tokenSymbol: String,
        val tokensCount: Int,
        val totalCryptoBalance: String,
        val totalFiatBalance: String,
        val isBalanceHidden: Boolean,
        val children: ImmutableList<GroupedChild>,
        override val onClick: () -> Unit,
    ) : UserAssetItemUM

    data class GroupedChild(
        val walletName: String,
        val accountName: AccountNameUM,
        val accountIcon: CryptoPortfolioIcon.Icon,
        val accountColor: CryptoPortfolioIcon.Color,
        val cryptoBalance: String,
        val fiatBalance: String,
    )
}