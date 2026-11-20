package com.tangem.store.features.cardsettings.impl.coderecovery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.store.features.cardsettings.impl.coderecovery.api.AccessCodeRecoveryComponent
import com.tangem.store.features.cardsettings.impl.coderecovery.model.AccessCodeRecoveryModel
import com.tangem.store.features.cardsettings.impl.coderecovery.ui.AccessCodeRecoveryScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Suppress("UnusedPrivateMember")
internal class DefaultAccessCodeRecoveryComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: Unit,
) : AccessCodeRecoveryComponent, AppComponentContext by appComponentContext {

    private val model: AccessCodeRecoveryModel = getOrCreateModel()

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        AccessCodeRecoveryScreen(
            state = state,
            onBackClick = { router.pop() },
        )
    }

    @AssistedFactory
    interface Factory : AccessCodeRecoveryComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultAccessCodeRecoveryComponent
    }
}