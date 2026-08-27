package com.tangem.features.pushnotifications.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.features.pushnotifications.api.PushNotificationsBottomSheetComponent
import com.tangem.features.pushnotifications.api.PushNotificationsParams
import com.tangem.features.pushnotifications.impl.model.PushNotificationsModel
import com.tangem.features.pushnotifications.impl.presentation.ui.PushNotificationsBottomSheet
import com.tangem.features.pushnotifications.impl.presentation.ui.PushNotificationsContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultPushNotificationsBottomSheetComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: PushNotificationsParams,
) : PushNotificationsBottomSheetComponent, AppComponentContext by appComponentContext {

    private val model: PushNotificationsModel = getOrCreateModel(params)

    @AssistedFactory
    interface Factory : PushNotificationsBottomSheetComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: PushNotificationsParams,
        ): DefaultPushNotificationsBottomSheetComponent
    }

    override fun dismiss() {
        params.modelCallbacks.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val bottomSheetConfig = remember(key1 = this) {
            TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = ::dismiss,
                content = TangemBottomSheetConfigContent.Empty,
            )
        }

        PushNotificationsBottomSheet(
            config = bottomSheetConfig,
        ) {
            PushNotificationsContent(
                onAllowClick = model::onAllowClick,
                onLaterClick = model::onLaterClick,
                onAllowPermission = model::onAllowPermission,
                onDenyPermission = model::onDenyPermission,
            )
        }
    }
}