package com.tangem.features.onramp.tokenlist.entity.utils

import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

fun List<TokensListItemUM>.addHeader(textReference: TextReference): ImmutableList<TokensListItemUM> {
    val items = this@addHeader

    return buildList {
        if (items.isNotEmpty()) {
            createGroupTitle(textReference = textReference).let(::add)
        }

        addAll(items)
    }
        .toImmutableList()
}

private fun createGroupTitle(textReference: TextReference): TokensListItemUM.GroupTitle {
    return TokensListItemUM.GroupTitle(id = textReference.hashCode(), text = textReference)
}