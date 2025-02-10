package com.tangem.feature.swap

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.features.swap.SwapComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Suppress("UnusedPrivateMember")
internal class DefaultSwapComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: SwapComponent.Params,
) : SwapComponent, AppComponentContext by appComponentContext {

    @Composable
    override fun Content(modifier: Modifier) {
        TODO("Not yet implemented")
    }

    @AssistedFactory
    interface Factory : SwapComponent.Factory {
        override fun create(context: AppComponentContext, params: SwapComponent.Params): DefaultSwapComponent
    }
}