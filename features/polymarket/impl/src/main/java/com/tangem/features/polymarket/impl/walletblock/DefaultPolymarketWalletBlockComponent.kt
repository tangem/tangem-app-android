package com.tangem.features.polymarket.impl.walletblock

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.tangem.core.ui.test.MainScreenTestTags
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.features.polymarket.api.walletblock.PolymarketWalletBlockComponent
import com.tangem.features.polymarket.api.walletblock.PolymarketWalletBlockUM
import com.tangem.features.polymarket.impl.walletblock.ui.PolymarketWalletBlockContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal const val PREDICTION_ACCOUNT_ROW_ID = "polymarket_prediction_account"
private const val PREDICTION_ACCOUNT_CONTENT_TYPE = "PredictionAccount"

@Suppress("UnusedPrivateProperty")
internal class DefaultPolymarketWalletBlockComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: Unit,
) : PolymarketWalletBlockComponent, AppComponentContext by context {

    override fun LazyListScope.polymarketWalletBlockContent(
        state: PolymarketWalletBlockUM,
        isBalanceHidden: Boolean,
        modifier: Modifier,
    ) {
        if (state is PolymarketWalletBlockUM.Hidden) return

        item(
            key = PREDICTION_ACCOUNT_ROW_ID,
            contentType = PREDICTION_ACCOUNT_CONTENT_TYPE,
        ) {
            PolymarketWalletBlockContent(
                state = state,
                isBalanceHidden = isBalanceHidden,
                modifier = modifier
                    .animateContentSize()
                    .testTag(MainScreenTestTags.ACCOUNT_LIST_ITEM),
            )
        }
    }

    @AssistedFactory
    interface Factory : PolymarketWalletBlockComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultPolymarketWalletBlockComponent
    }
}