package com.tangem.feature.wallet.presentation.wallet.state.model

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds.row.TangemRowUM
import com.tangem.core.ui.ds.row.header.TangemHeaderRowUM
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
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
    val tokenRowUM: TangemRowUM

    data class GroupTitle(
        override val tokenRowUM: TangemHeaderRowUM,
    ) : TokensListItemUM2

    data class Token(
        override val tokenRowUM: TangemTokenRowUM,
    ) : TokensListItemUM2

    data class Portfolio(
        override val tokenRowUM: TangemTokenRowUM,
        val onEmptyClick: () -> Unit,
        val tokenList: ImmutableList<TokensListItemUM2>,
        val isExpanded: Boolean,
        val isCollapsable: Boolean,
    ) : TokensListItemUM2
}