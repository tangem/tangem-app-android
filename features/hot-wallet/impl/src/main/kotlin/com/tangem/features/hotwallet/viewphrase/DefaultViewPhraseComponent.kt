package com.tangem.features.hotwallet.viewphrase

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.hotwallet.ViewPhraseComponent
import com.tangem.features.hotwallet.viewphrase.model.ViewPhraseModel
import com.tangem.features.hotwallet.viewphrase.ui.ViewPhraseContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultViewPhraseComponent @AssistedInject constructor(
    @Assisted private val context: AppComponentContext,
    @Assisted private val params: ViewPhraseComponent.Params,
) : ViewPhraseComponent, AppComponentContext by context {
    private val model: ViewPhraseModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        ViewPhraseContent(
            state = state,
            modifier = modifier,
        )
    }

    @AssistedFactory
    interface Factory : ViewPhraseComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: ViewPhraseComponent.Params,
        ): DefaultViewPhraseComponent
    }
}