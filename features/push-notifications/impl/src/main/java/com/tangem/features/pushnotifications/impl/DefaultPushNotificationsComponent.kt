package com.tangem.features.pushnotifications.impl

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.NavigationBar3ButtonsScrim
import com.tangem.core.ui.utils.findActivity
import com.tangem.features.pushnotifications.api.PushNotificationsComponent
import com.tangem.features.pushnotifications.api.PushNotificationsParams
import com.tangem.features.pushnotifications.impl.model.PushNotificationsModel
import com.tangem.features.pushnotifications.impl.presentation.ui.PushNotificationsScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultPushNotificationsComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: PushNotificationsParams,
) : PushNotificationsComponent, AppComponentContext by appComponentContext {

    private val model: PushNotificationsModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val activity = LocalContext.current.findActivity()
        val state by model.state.collectAsState()
        BackHandler(onBack = { activity.finish() })
        NavigationBar3ButtonsScrim()
        PushNotificationsScreen(
            onAllowClick = model::onAllowClick,
            onLaterClick = model::onLaterClick,
            onAllowPermission = model::onAllowPermission,
            onDenyPermission = model::onDenyPermission,
            showNotificationsInfo = state.showInfoAboutNotifications,
        )
    }

    @AssistedFactory
    interface Factory : PushNotificationsComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: PushNotificationsParams,
        ): DefaultPushNotificationsComponent
    }
}