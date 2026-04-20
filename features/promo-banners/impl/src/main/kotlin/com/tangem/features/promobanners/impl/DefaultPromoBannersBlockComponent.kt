package com.tangem.features.promobanners.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.promobanners.api.PromoBannersBlockComponent
import com.tangem.features.promobanners.impl.model.PromoBannersBlockModel
import com.tangem.features.promobanners.impl.ui.PromoBannersBlock
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultPromoBannersBlockComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: PromoBannersBlockComponent.Params,
) : PromoBannersBlockComponent, AppComponentContext by appComponentContext {

    private val model: PromoBannersBlockModel = getOrCreateModel(params)

    override fun setVisibleOnScreen(isVisible: Boolean) {
        model.setVisibleOnScreen(isVisible)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        PromoBannersBlock(
            state = state,
            modifier = modifier,
        )
    }

    @AssistedFactory
    interface Factory : PromoBannersBlockComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: PromoBannersBlockComponent.Params,
        ): DefaultPromoBannersBlockComponent
    }
}