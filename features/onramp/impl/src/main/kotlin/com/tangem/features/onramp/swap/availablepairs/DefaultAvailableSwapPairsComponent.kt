package com.tangem.features.onramp.swap.availablepairs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.onramp.swap.availablepairs.model.AvailableSwapPairsModel
import com.tangem.features.onramp.tokenlist.ui.TokenList
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Stable
internal class DefaultAvailableSwapPairsComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: AvailableSwapPairsComponent.Params,
) : AvailableSwapPairsComponent, AppComponentContext by context {

    private val model: AvailableSwapPairsModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()

        TokenList(state = state, modifier = modifier)
    }

    @AssistedFactory
    interface Factory : AvailableSwapPairsComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: AvailableSwapPairsComponent.Params,
        ): DefaultAvailableSwapPairsComponent
    }
}