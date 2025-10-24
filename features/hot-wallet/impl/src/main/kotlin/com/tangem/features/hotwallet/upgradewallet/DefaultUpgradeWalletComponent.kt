package com.tangem.features.hotwallet.upgradewallet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.hotwallet.UpgradeWalletComponent
import com.tangem.features.hotwallet.upgradewallet.ui.UpgradeWalletContent
import com.tangem.features.onboarding.v2.util.ResetCardsComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Suppress("UnusedPrivateMember")
internal class DefaultUpgradeWalletComponent @AssistedInject constructor(
    @Assisted private val context: AppComponentContext,
    @Assisted private val params: UpgradeWalletComponent.Params,
    resetCardsComponentFactory: ResetCardsComponent.Factory,
) : UpgradeWalletComponent, AppComponentContext by context {

    private val model: UpgradeWalletModel = getOrCreateModel(params)

    private val resetCardsComponent = resetCardsComponentFactory.create(
        context = child("ResetCardsComponent"),
        params = ResetCardsComponent.Params(
            callbacks = model.resetCardsComponentCallbacks,
        ),
    )

    init {
        model.startResetCardsFlow
            .onEach { resetCardsComponent.startResetCardsFlow(it) }
            .launchIn(componentScope)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        UpgradeWalletContent(
            state = state,
            modifier = modifier,
        )
    }

    @AssistedFactory
    interface Factory : UpgradeWalletComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: UpgradeWalletComponent.Params,
        ): DefaultUpgradeWalletComponent
    }
}