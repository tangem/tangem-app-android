package com.tangem.feature.walletsettings.component.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.feature.walletsettings.component.NetworksAvailableForNotificationsComponent
import com.tangem.feature.walletsettings.component.impl.model.NetworksAvailableForNotificationsModel
import com.tangem.feature.walletsettings.ui.NetworksAvailableForNotificationsListBS
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultNetworksAvailableForNotificationsComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: NetworksAvailableForNotificationsComponent.Params,
) : NetworksAvailableForNotificationsComponent, AppComponentContext by context {

    private val model: NetworksAvailableForNotificationsModel = getOrCreateModel(params)

    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.state.collectAsStateWithLifecycle()

        NetworksAvailableForNotificationsListBS(state = state, onBack = ::dismiss, onDismiss = ::dismiss)
    }

    @AssistedFactory
    interface Factory : NetworksAvailableForNotificationsComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: NetworksAvailableForNotificationsComponent.Params,
        ): DefaultNetworksAvailableForNotificationsComponent
    }
}