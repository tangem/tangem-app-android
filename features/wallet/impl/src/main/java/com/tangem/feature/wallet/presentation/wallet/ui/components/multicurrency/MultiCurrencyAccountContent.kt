package com.tangem.feature.wallet.presentation.wallet.ui.components.multicurrency

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import com.tangem.common.ui.tokens.portfolioTokensList
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.test.MainScreenTestTags
import kotlinx.collections.immutable.ImmutableList

internal fun LazyListScope.portfolioContentItems(
    items: ImmutableList<TokensListItemUM.Portfolio>,
    modifier: Modifier = Modifier,
    isBalanceHidden: Boolean,
) {
    items.forEachIndexed { index, item ->
        portfolioTokensList(
            portfolio = item,
            modifier = modifier,
            portfolioIndex = index,
            isBalanceHidden = isBalanceHidden,
            testTag = MainScreenTestTags.TOKEN_LIST_ITEM,
        )
    }
}