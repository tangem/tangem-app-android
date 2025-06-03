package com.tangem.feature.stories.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.common.ui.swapStoriesScreen.SwapStoriesScreen
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.utils.ChangeRootBackgroundColorEffect
import com.tangem.feature.stories.api.StoriesComponent
import com.tangem.feature.stories.impl.model.StoriesModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Stable
internal class DefaultStoriesComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: StoriesComponent.Params,
) : StoriesComponent, AppComponentContext by context {

    private val model: StoriesModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state = model.state.collectAsStateWithLifecycle()
        SwapStoriesScreen(state.value)
        ChangeRootBackgroundColorEffect(TangemColorPalette.Black)
    }

    @AssistedFactory
    interface Factory : StoriesComponent.Factory {
        override fun create(context: AppComponentContext, params: StoriesComponent.Params): DefaultStoriesComponent
    }
}