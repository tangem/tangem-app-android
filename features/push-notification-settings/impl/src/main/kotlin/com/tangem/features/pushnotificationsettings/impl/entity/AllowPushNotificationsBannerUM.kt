package com.tangem.features.pushnotificationsettings.impl.entity

import androidx.compose.runtime.Immutable

@Immutable
internal data class AllowPushNotificationsBannerUM(
    val onOpenSettingsClick: () -> Unit,
)