package com.tangem.features.collectibles.impl.walletblock

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.test.MainScreenTestTags
import com.tangem.features.collectibles.api.walletblock.CollectiblesWalletBlockComponent
import com.tangem.features.collectibles.api.walletblock.CollectiblesWalletBlockUM
import com.tangem.features.collectibles.impl.walletblock.ui.CollectiblesWalletBlockContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal const val COLLECTIBLES_ACCOUNT_ROW_ID = "collectibles_account"
private const val COLLECTIBLES_ACCOUNT_CONTENT_TYPE = "CollectiblesAccount"

@Suppress("UnusedPrivateProperty")
internal class DefaultCollectiblesWalletBlockComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: Unit,
) : CollectiblesWalletBlockComponent, AppComponentContext by context {

    override fun LazyListScope.collectiblesWalletBlockContent(
        state: CollectiblesWalletBlockUM,
        isBalanceHidden: Boolean,
        modifier: Modifier,
    ) {
        if (state is CollectiblesWalletBlockUM.Hidden) return

        item(
            key = COLLECTIBLES_ACCOUNT_ROW_ID,
            contentType = COLLECTIBLES_ACCOUNT_CONTENT_TYPE,
        ) {
            CollectiblesWalletBlockContent(
                state = state,
                isBalanceHidden = isBalanceHidden,
                modifier = modifier
                    .animateContentSize()
                    .testTag(MainScreenTestTags.ACCOUNT_LIST_ITEM),
            )
        }
    }

    @AssistedFactory
    interface Factory : CollectiblesWalletBlockComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultCollectiblesWalletBlockComponent
    }
}