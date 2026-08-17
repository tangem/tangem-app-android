package com.tangem.features.polymarket.main.impl.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.features.polymarket.main.api.PolymarketMainBlockComponent
import com.tangem.features.polymarket.main.api.entity.PolymarketMainUM
import com.tangem.features.polymarket.main.impl.ui.PolymarketMainBlockContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

private const val PREDICTION_ACCOUNT_KEY = "polymarket_prediction_account"
private const val PREDICTION_ACCOUNT_CONTENT_TYPE = "PredictionAccount"

@Suppress("UnusedPrivateProperty")
internal class DefaultPolymarketMainBlockComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: Unit,
) : PolymarketMainBlockComponent, AppComponentContext by context {

    override fun LazyListScope.polymarketMainContent(
        state: PolymarketMainUM,
        isBalanceHidden: Boolean,
        modifier: Modifier,
    ) {
        if (state is PolymarketMainUM.Hidden) return

        item(
            key = PREDICTION_ACCOUNT_KEY,
            contentType = PREDICTION_ACCOUNT_CONTENT_TYPE,
        ) {
            PolymarketMainBlockContent(
                state = state,
                isBalanceHidden = isBalanceHidden,
                modifier = modifier.animateContentSize(),
            )
        }
    }

    @AssistedFactory
    interface Factory : PolymarketMainBlockComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultPolymarketMainBlockComponent
    }
}