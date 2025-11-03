package com.tangem.features.onramp.tokenlist.entity.transformer

import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.onramp.tokenlist.entity.TokenListUM
import com.tangem.features.onramp.tokenlist.entity.TokenListUMData
import com.tangem.features.onramp.tokenlist.entity.TokenListUMTransformer
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

internal class SetNothingToFoundStateTransformerV2(
    private val isBalanceHidden: Boolean,
    private val emptySearchMessageReference: TextReference,
) : TokenListUMTransformer {

    override fun transform(prevState: TokenListUM): TokenListUM {
        return prevState.copy(
            availableItems = persistentListOf(),
            unavailableItems = persistentListOf(),
            tokensListData = TokenListUMData.TokenList(tokensList = buildList {
                TokensListItemUM.Text(
                    id = emptySearchMessageReference.hashCode(),
                    text = emptySearchMessageReference,
                ).let(::add)
            }.toImmutableList()),
            isBalanceHidden = isBalanceHidden,
        )
    }
}