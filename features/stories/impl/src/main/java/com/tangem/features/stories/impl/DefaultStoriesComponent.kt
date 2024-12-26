package com.tangem.features.stories.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.features.stories.api.component.StoriesComponent
import com.tangem.features.stories.impl.ui.StoriesComponentContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

// todo Temporary remove in [REDACTED_TASK_KEY]
@Suppress("UnusedPrivateMember")
internal class DefaultStoriesComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: StoriesComponent.Params,
) : StoriesComponent, AppComponentContext by appComponentContext {

    @Composable
    override fun Content(modifier: Modifier) {
        StoriesComponentContent()
    }

    @AssistedFactory
    interface Factory : StoriesComponent.Factory {
        override fun create(context: AppComponentContext, params: StoriesComponent.Params): DefaultStoriesComponent
    }
}