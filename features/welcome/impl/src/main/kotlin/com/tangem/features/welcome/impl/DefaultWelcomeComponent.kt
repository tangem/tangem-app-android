package com.tangem.features.welcome.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import com.tangem.features.introduction.AddWalletComponent
import com.tangem.features.welcome.WelcomeComponent
import com.tangem.features.welcome.impl.model.WelcomeModel
import com.tangem.features.welcome.impl.ui.Welcome
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch

internal class DefaultWelcomeComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted val params: Unit,
    hotWalletFeatureToggles: HotWalletFeatureToggles,
    addWalletComponentFactory: AddWalletComponent.Factory,
) : WelcomeComponent, AppComponentContext by context {

    private val model: WelcomeModel = getOrCreateModel()

    private val addWalletComponent: AddWalletComponent? = if (hotWalletFeatureToggles.isMultipleHotWalletsEnabled) {
        addWalletComponentFactory.create(
            context = child(key = "add_wallet"),
            params = AddWalletComponent.Params(callbacks = model.addWalletModelCallbacks),
        )
    } else {
        null
    }

    init {
        addWalletComponent?.let { component ->
            componentScope.launch { model.addWalletSheetRequests.collect { component.open() } }
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        Box(modifier = modifier) {
            Welcome(
                modifier = Modifier.fillMaxSize(),
                state = state,
            )
            addWalletComponent?.Content(modifier = Modifier.fillMaxSize())
        }
    }

    @AssistedFactory
    interface Factory : WelcomeComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultWelcomeComponent
    }
}