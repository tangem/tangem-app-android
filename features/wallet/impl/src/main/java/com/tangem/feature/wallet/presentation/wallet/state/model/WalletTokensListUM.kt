package com.tangem.feature.wallet.presentation.wallet.state.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds.row.header.TangemHeaderRowUM
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.ds2.accountrow.TangemAccountRow
import com.tangem.core.ui.ds2.avatar.TangemAvatar
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.jointaccount.main.JointAccountMainUM
import com.tangem.utils.annotations.RemoveWithToggle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * State of the tokens list in the wallet screen
 *
 * @property tokenList list of tokens to display
 * @property organizeButtonUM configuration for the "Organize Tokens" button, if it should
 */
@Immutable
internal sealed class WalletTokensListUM {

    abstract val tokenList: ImmutableList<TokensListItemUM2>
    abstract val organizeButtonUM: TangemButtonUM?

    data class Empty(
        val onEmptyClick: () -> Unit,
    ) : WalletTokensListUM() {
        override val tokenList: ImmutableList<TokensListItemUM2.Portfolio> = persistentListOf()
        override val organizeButtonUM: TangemButtonUM? = null
    }

    data object Locked : WalletTokensListUM() {
        override val tokenList: ImmutableList<TokensListItemUM2.Portfolio> = persistentListOf(
            TokensListItemUM2.Portfolio(
                tokenRowUM = TangemTokenRowUM.Empty(id = "0"),
                tokenList = persistentListOf(),
                isExpanded = false,
                isCollapsable = true,
                onEmptyClick = {},
            ),
            TokensListItemUM2.Portfolio(
                tokenRowUM = TangemTokenRowUM.Empty(id = "1"),
                tokenList = persistentListOf(),
                isExpanded = false,
                isCollapsable = true,
                onEmptyClick = {},
            ),
        )
        override val organizeButtonUM: TangemButtonUM? = null
    }

    data object Loading : WalletTokensListUM() {
        override val tokenList: ImmutableList<TokensListItemUM2> = persistentListOf(
            TokensListItemUM2.Portfolio(
                tokenRowUM = TangemTokenRowUM.Loading(id = "0"),
                tokenList = persistentListOf(),
                isExpanded = false,
                isCollapsable = true,
                onEmptyClick = {},
            ),
            TokensListItemUM2.Portfolio(
                tokenRowUM = TangemTokenRowUM.Loading(id = "1"),
                tokenList = persistentListOf(),
                isExpanded = false,
                isCollapsable = true,
                onEmptyClick = {},
            ),
            TokensListItemUM2.Portfolio(
                tokenRowUM = TangemTokenRowUM.Loading(id = "2"),
                tokenList = persistentListOf(),
                isExpanded = false,
                isCollapsable = true,
                onEmptyClick = {},
            ),
        )
        override val organizeButtonUM: TangemButtonUM? = null
    }

    data class Content(
        override val tokenList: ImmutableList<TokensListItemUM2>,
        override val organizeButtonUM: TangemButtonUM?,
    ) : WalletTokensListUM()
}

/**
 * State of token list item in the wallet screen
 */
@Immutable
internal sealed interface TokensListItemUM2 {
    val id: String

    data class GroupTitle(
        val tokenRowUM: TangemHeaderRowUM,
    ) : TokensListItemUM2 {
        override val id: String = tokenRowUM.id
    }

    data class Token(
        val tokenRowUM: TangemTokenRowUM,
    ) : TokensListItemUM2 {
        override val id: String = tokenRowUM.id
    }

    @RemoveWithToggle("AND_18663_REDESIGNED_ACCOUNT_ROWS_ENABLED")
    data class Portfolio(
        val tokenRowUM: TangemTokenRowUM,
        val onEmptyClick: () -> Unit,
        val tokenList: ImmutableList<TokensListItemUM2>,
        val isExpanded: Boolean,
        val isCollapsable: Boolean,
    ) : TokensListItemUM2 {
        override val id: String = tokenRowUM.id
    }

    data class AccountV2(
        val state: TangemAccountRow.State,
        val avatar: TangemAvatar.UiState,
        val isExpanded: Boolean,
        val shouldShowCompactBalance: Boolean,
        val onEmptyClick: () -> Unit,
        val tokenList: ImmutableList<TokensListItemUM2>,
    ) : TokensListItemUM2 {
        override val id: String = state.id
    }

    data class SectionV2(
        override val id: String,
        val title: TextReference,
    ) : TokensListItemUM2

    data class TokenV2(
        val state: TangemTokenRow.State,
        val onLongClick: ((Offset, TangemTokenRow.State) -> Unit)?,
        val onPromoShown: (() -> Unit)? = null,
    ) : TokensListItemUM2 {
        override val id: String = state.id
    }

    /**
     * A joint account that is not yet activated (collecting members / loading / blocked / temporarily
     * unavailable). Rendered as a single non-collapsible row by the joint-account feature component,
     * keyed by [accountId] so multiple pending joint accounts can coexist in the list. Once activated,
     * a joint account becomes a regular [Portfolio] instead.
     */
    data class JointPending(
        val accountId: String,
        val state: JointAccountMainUM,
    ) : TokensListItemUM2 {
        override val id: String = accountId
    }
}