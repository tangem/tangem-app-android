package com.tangem.features.pushnotificationsettings.impl.entity

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList

@Immutable
internal sealed interface PushNotificationSettingsUM {

    data object Loading : PushNotificationSettingsUM

    data class Content(
        val banner: AllowPushNotificationsBannerUM?,
        val toggles: PersistentList<ToggleUM>,
        val onMoreInfoClick: () -> Unit,
    ) : PushNotificationSettingsUM

    data class Error(
        val onRetryClick: () -> Unit,
    ) : PushNotificationSettingsUM
}