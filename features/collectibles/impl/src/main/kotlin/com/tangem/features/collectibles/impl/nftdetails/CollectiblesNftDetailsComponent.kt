package com.tangem.features.collectibles.impl.nftdetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.models.wallet.UserWalletId
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class CollectiblesNftDetailsComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: Params,
) : ComposableContentComponent, AppComponentContext by context {

    @Composable
    override fun Content(modifier: Modifier) {
        Box(modifier = modifier.fillMaxSize().background(TangemTheme.colors3.bg.primary))
    }

    data class Params(val userWalletId: UserWalletId, val id: String)

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext, params: Params): CollectiblesNftDetailsComponent
    }
}