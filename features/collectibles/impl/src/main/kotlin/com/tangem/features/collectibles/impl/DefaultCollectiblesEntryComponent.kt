package com.tangem.features.collectibles.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.collectibles.api.CollectiblesEntryComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultCollectiblesEntryComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted @Suppress("UnusedPrivateProperty") private val params: Unit,
) : CollectiblesEntryComponent, AppComponentContext by context {

    @Composable
    override fun Content(modifier: Modifier) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(TangemTheme.colors3.bg.primary),
        )
    }

    @AssistedFactory
    interface Factory : CollectiblesEntryComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultCollectiblesEntryComponent
    }
}