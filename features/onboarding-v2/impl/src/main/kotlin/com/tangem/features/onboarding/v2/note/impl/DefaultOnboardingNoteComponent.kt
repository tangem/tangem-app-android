package com.tangem.features.onboarding.v2.note.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.features.onboarding.v2.note.api.OnboardingNoteComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultOnboardingNoteComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted val params: OnboardingNoteComponent.Params,
) : OnboardingNoteComponent, AppComponentContext by context {

    @Composable
    override fun Content(modifier: Modifier) {
// [REDACTED_TODO_COMMENT]
    }

    @AssistedFactory
    interface Factory : OnboardingNoteComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: OnboardingNoteComponent.Params,
        ): DefaultOnboardingNoteComponent
    }
}
