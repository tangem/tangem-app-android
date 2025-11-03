package com.tangem.features.onramp.swap.availablepairs

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.onramp.swap.availablepairs.model.AvailableSwapPairsModel
import com.tangem.features.onramp.tokenlist.entity.TokenListUM
import com.tangem.features.onramp.tokenlist.ui.onrampTokenList
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow

@Stable
internal class DefaultAvailableSwapPairsComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: AvailableSwapPairsComponent.Params,
) : AvailableSwapPairsComponent, AppComponentContext by context {

    private val model: AvailableSwapPairsModel = getOrCreateModel(params)

    override val uiState: StateFlow<TokenListUM>
        get() = model.state

    override fun LazyListScope.content(uiState: TokenListUM, modifier: Modifier) {
        onrampTokenList(state = uiState)
    }

    @AssistedFactory
    interface Factory : AvailableSwapPairsComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: AvailableSwapPairsComponent.Params,
        ): DefaultAvailableSwapPairsComponent
    }
}