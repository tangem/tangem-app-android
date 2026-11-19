package com.tangem.features.collectibles.impl.nftdetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.collectibles.impl.nftdetails.model.CollectiblesNftDetailsModel
import com.tangem.features.collectibles.impl.nftdetails.ui.CollectiblesNftDetailsScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class CollectiblesNftDetailsComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: Params,
) : ComposableContentComponent, AppComponentContext by context {

    private val model: CollectiblesNftDetailsModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        CollectiblesNftDetailsScreen(
            state = state,
            onBackClick = model::onBackClick,
            onCloseClick = model::onCloseClick,
            onBuybackClick = model::onBuybackClick,
            modifier = modifier
        )
    }

    data class Params(val userWalletId: UserWalletId, val id: String)

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext, params: Params): CollectiblesNftDetailsComponent
    }
}