package com.tangem.features.pushnotifications.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface PushNotificationsComponent : ComposableContentComponent {

    interface Factory : ComponentFactory<PushNotificationsParams, PushNotificationsComponent>
}