package com.tangem.features.polymarket.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.features.polymarket.api.PolymarketComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultPolymarketComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: PolymarketComponent.Params,
) : PolymarketComponent, AppComponentContext by appComponentContext {

    @Composable
    override fun Content(modifier: Modifier) {
        // TODO([REDACTED_TASK_KEY]): real Polymarket prediction account UI
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = params.userWalletId.stringValue)
        }
    }

    @AssistedFactory
    interface Factory : PolymarketComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: PolymarketComponent.Params,
        ): DefaultPolymarketComponent
    }
}