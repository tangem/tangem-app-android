package com.tangem.features.commonfeatures.api.choosetoken.model

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.core.ui.ds2.accountrow.TangemAccountRow
import com.tangem.core.ui.ds2.avatar.TangemAvatar
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow
import com.tangem.core.ui.extensions.TextReference
import com.tangem.utils.annotations.RemoveWithToggle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

data class ChooseTokenPortfolioFullBlockUM(
    val walletList: WalletListUM,
    val isBalanceHidden: Boolean,
    val isSearching: Boolean,
    val tokensListData: TokenListUMData,
    val balanceFilterUM: BalanceFilterUM? = null,
)

data class WalletListUM(
    val items: ImmutableList<WalletTabUM>,
)

data class WalletTabUM(
    val text: TextReference,
    val count: TextReference?,
    val isSelected: Boolean,
    val onClick: () -> Unit,
    val deviceIcon: DeviceIconUM,
)

@Immutable
data class AccountRowUM(
    val state: TangemAccountRow.State,
    val avatar: TangemAvatar.UiState,
    val tokens: ImmutableList<TangemTokenRow.State>,
)

@Immutable
sealed interface TokenRowItemUM {
    val id: String

    data class Token(val state: TangemTokenRow.State) : TokenRowItemUM {
        override val id: String = state.id
    }

    data class Section(override val id: String, val title: TextReference) : TokenRowItemUM
}

@Immutable
sealed interface TokenListUMData {

    val tokensList: ImmutableList<TokensListItemUM>
    val totalTokensCount: Int

    data class AccountList(
        override val tokensList: ImmutableList<TokensListItemUM.Portfolio>,
        override val totalTokensCount: Int,
        @RemoveWithToggle(toggleName = "AND_18671_REDESIGNED_TOKEN_ROWS_ENABLED", description = "Drop the default")
        val accountRows: ImmutableMap<String, AccountRowUM> = persistentMapOf(),
    ) : TokenListUMData

    data class TokenList(
        override val tokensList: ImmutableList<TokensListItemUM>,
        override val totalTokensCount: Int,
        @RemoveWithToggle(toggleName = "AND_18671_REDESIGNED_TOKEN_ROWS_ENABLED", description = "Drop the default")
        val rows: ImmutableList<TokenRowItemUM> = persistentListOf(),
    ) : TokenListUMData

    data class EmptyList(
        val reason: EmptyReason = EmptyReason.NoTokens,
    ) : TokenListUMData {
        override val tokensList: ImmutableList<TokensListItemUM> = persistentListOf()
        override val totalTokensCount: Int = EMPTY_TOKENS_COUNT
    }

    private companion object {
        const val EMPTY_TOKENS_COUNT = 0
    }
}