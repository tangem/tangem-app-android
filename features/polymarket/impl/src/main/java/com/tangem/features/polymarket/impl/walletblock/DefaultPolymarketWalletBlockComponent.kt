package com.tangem.features.polymarket.impl.walletblock

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.features.polymarket.api.walletblock.PolymarketWalletBlockComponent
import com.tangem.features.polymarket.api.walletblock.PolymarketWalletBlockUM
import com.tangem.features.polymarket.impl.walletblock.ui.PolymarketWalletBlockContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

private const val PREDICTION_ACCOUNT_KEY = "polymarket_prediction_account"
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
            key = PREDICTION_ACCOUNT_KEY,
            contentType = PREDICTION_ACCOUNT_CONTENT_TYPE,
        ) {
            PolymarketWalletBlockContent(
                state = state,
                isBalanceHidden = isBalanceHidden,
                modifier = modifier.animateContentSize(),
            )
        }
    }

    @AssistedFactory
    interface Factory : PolymarketWalletBlockComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultPolymarketWalletBlockComponent
    }
}