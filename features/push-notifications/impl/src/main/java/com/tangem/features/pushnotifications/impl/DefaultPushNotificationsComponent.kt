package com.tangem.features.pushnotifications.impl

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.NavigationBar3ButtonsScrim
import com.tangem.core.ui.utils.findActivity
import com.tangem.features.pushnotifications.api.PushNotificationsComponent
import com.tangem.features.pushnotifications.impl.model.PushNotificationsModel
import com.tangem.features.pushnotifications.impl.presentation.ui.PushNotificationsScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultPushNotificationsComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: Unit,
) : PushNotificationsComponent, AppComponentContext by appComponentContext {

    private val model: PushNotificationsModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val activity = LocalContext.current.findActivity()
        BackHandler(onBack = { activity.finish() })
        NavigationBar3ButtonsScrim()
        PushNotificationsScreen(
            onRequest = model::onRequest,
            onNeverRequest = model::onNeverRequest,
            onAllowPermission = model::onAllowPermission,
            onDenyPermission = model::onDenyPermission,
        )
    }

    @AssistedFactory
    interface Factory : PushNotificationsComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultPushNotificationsComponent
    }
}