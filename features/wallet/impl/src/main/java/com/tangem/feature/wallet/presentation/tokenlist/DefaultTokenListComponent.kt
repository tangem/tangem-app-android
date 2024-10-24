package com.tangem.feature.wallet.presentation.tokenlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.feature.wallet.presentation.tokenlist.model.TokenListModel
import com.tangem.feature.wallet.presentation.tokenlist.ui.TokenList
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Stable
internal class DefaultTokenListComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: TokenListComponent.Params,
) : TokenListComponent, AppComponentContext by context {

    private val model: TokenListModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()

        TokenList(state = state, modifier = modifier)
    }

    @AssistedFactory
    interface Factory : TokenListComponent.Factory {
        override fun create(context: AppComponentContext, params: TokenListComponent.Params): DefaultTokenListComponent
    }
}