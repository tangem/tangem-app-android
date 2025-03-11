package com.tangem.features.onramp.tokenlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.onramp.tokenlist.model.OnrampTokenListModel
import com.tangem.features.onramp.tokenlist.ui.TokenList
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Stable
internal class DefaultOnrampTokenListComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: OnrampTokenListComponent.Params,
) : OnrampTokenListComponent, AppComponentContext by context {

    private val model: OnrampTokenListModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()

        TokenList(state = state, modifier = modifier)
    }

    @AssistedFactory
    interface Factory : OnrampTokenListComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: OnrampTokenListComponent.Params,
        ): DefaultOnrampTokenListComponent
    }
}