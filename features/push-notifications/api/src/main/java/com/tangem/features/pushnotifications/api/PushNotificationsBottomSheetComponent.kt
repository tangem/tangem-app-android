package com.tangem.features.pushnotifications.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent

interface PushNotificationsBottomSheetComponent : ComposableBottomSheetComponent {

    interface Factory : ComponentFactory<PushNotificationsParams, PushNotificationsBottomSheetComponent>
}