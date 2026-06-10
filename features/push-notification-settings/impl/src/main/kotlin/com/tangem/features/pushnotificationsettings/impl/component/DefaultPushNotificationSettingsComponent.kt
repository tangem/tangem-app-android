package com.tangem.features.pushnotificationsettings.impl.component

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.essenty.lifecycle.doOnResume
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.feature.walletsettings.component.NetworksAvailableForNotificationsComponent
import com.tangem.features.pushnotifications.api.utils.getPushPermissionOrNull
import com.tangem.features.pushnotificationsettings.component.PushNotificationSettingsComponent
import com.tangem.features.pushnotificationsettings.impl.entity.NetworksAvailableForNotificationBSConfig
import com.tangem.features.pushnotificationsettings.impl.model.PushNotificationSettingsModel
import com.tangem.features.pushnotificationsettings.impl.ui.PushNotificationSettingsScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultPushNotificationSettingsComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: PushNotificationSettingsComponent.Params,
    private val networksAvailableForNotificationsComponentFactory: NetworksAvailableForNotificationsComponent.Factory,
) : PushNotificationSettingsComponent, AppComponentContext by context {

    private val model: PushNotificationSettingsModel = getOrCreateModel(params)

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = null,
        handleBackButton = false,
        key = "moreInfoBottomSheet",
        childFactory = ::bottomSheetChild,
    )

    init {
        lifecycle.doOnResume { model.onResume() }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = model::onPermissionResult,
        )

        LaunchedEffect(Unit) {
            model.requestPushPermission.collect {
                val permission = getPushPermissionOrNull()
                if (permission != null) {
                    permissionLauncher.launch(permission)
                } else {
                    model.onPermissionResult(isGranted = false)
                }
            }
        }

        PushNotificationSettingsScreen(
            modifier = modifier,
            state = state,
            onBackClick = router::pop,
        )

        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(
        @Suppress("UNUSED_PARAMETER") config: NetworksAvailableForNotificationBSConfig,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent = networksAvailableForNotificationsComponentFactory.create(
        context = childByContext(componentContext),
        params = NetworksAvailableForNotificationsComponent.Params(
            onDismiss = model.bottomSheetNavigation::dismiss,
        ),
    )

    @AssistedFactory
    interface Factory : PushNotificationSettingsComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: PushNotificationSettingsComponent.Params,
        ): DefaultPushNotificationSettingsComponent
    }
}