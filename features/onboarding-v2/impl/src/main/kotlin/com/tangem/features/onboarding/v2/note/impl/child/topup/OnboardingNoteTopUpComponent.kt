package com.tangem.features.onboarding.v2.note.impl.child.topup

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.onboarding.v2.note.impl.DefaultOnboardingNoteComponent
import com.tangem.features.onboarding.v2.note.impl.child.topup.model.OnboardingNoteTopUpModel
import com.tangem.features.onboarding.v2.note.impl.child.topup.ui.OnboardingNoteTopUp

internal class OnboardingNoteTopUpComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: OnboardingNoteTopUpModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        BackHandler(onBack = remember(this) { { params.childParams.onBack() } })

        OnboardingNoteTopUp(
            modifier = modifier,
            state = state,
        )
    }

    data class Params(
        val childParams: DefaultOnboardingNoteComponent.ChildParams,
        val onDone: () -> Unit,
    )
}