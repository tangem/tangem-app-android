package com.tangem.features.onramp.tokenlist.entity.transformer

import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.tokenlist.entity.TokenListUM
import com.tangem.features.onramp.tokenlist.entity.TokenListUMData
import com.tangem.features.onramp.tokenlist.entity.TokenListUMTransformer
import kotlinx.collections.immutable.persistentListOf

internal class SetNothingToFoundStateTransformer(
    private val isBalanceHidden: Boolean,
    private val emptySearchMessageReference: TextReference,
) : TokenListUMTransformer {

    override fun transform(prevState: TokenListUM): TokenListUM {
        return prevState.copy(
            availableItems = persistentListOf(
                createGroupTitle(
                    textReference = resourceReference(id = R.string.exchange_tokens_available_tokens_header),
                ),
                TokensListItemUM.Text(
                    id = emptySearchMessageReference.hashCode(),
                    text = emptySearchMessageReference,
                ),
            ),
            unavailableItems = persistentListOf(),
            tokensListData = TokenListUMData.EmptyList,
            isBalanceHidden = isBalanceHidden,
        )
    }

    private fun createGroupTitle(textReference: TextReference): TokensListItemUM.GroupTitle {
        return TokensListItemUM.GroupTitle(id = textReference.hashCode(), text = textReference)
    }
}