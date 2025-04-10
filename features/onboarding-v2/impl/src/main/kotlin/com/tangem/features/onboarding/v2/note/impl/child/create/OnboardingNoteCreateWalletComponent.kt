package com.tangem.features.onboarding.v2.note.impl.child.create

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.onboarding.v2.note.impl.DefaultOnboardingNoteComponent
import com.tangem.features.onboarding.v2.note.impl.child.create.model.OnboardingNoteCreateWalletModel
import com.tangem.features.onboarding.v2.note.impl.child.create.ui.OnboardingNoteCreateWallet

internal class OnboardingNoteCreateWalletComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: OnboardingNoteCreateWalletModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        BackHandler(onBack = remember(this) { { params.childParams.onBack } })

        OnboardingNoteCreateWallet(
            modifier = modifier,
            state = state,
        )
    }

    data class Params(
        val childParams: DefaultOnboardingNoteComponent.ChildParams,
        val onWalletCreated: (UserWallet) -> Unit,
    )
}