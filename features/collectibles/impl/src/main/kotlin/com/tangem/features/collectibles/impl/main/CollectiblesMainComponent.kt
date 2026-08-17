package com.tangem.features.collectibles.impl.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.collectibles.impl.main.ui.CollectiblesMainScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class CollectiblesMainComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
) : ComposableContentComponent, AppComponentContext by context {

    @Composable
    override fun Content(modifier: Modifier) {
        CollectiblesMainScreen(modifier = modifier)
    }

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext): CollectiblesMainComponent
    }
}