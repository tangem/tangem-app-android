package com.tangem.features.pushnotificationsettings.impl.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.UnableToLoadData
import com.tangem.features.pushnotificationsettings.impl.entity.PushNotificationSettingsUM

@Composable
internal fun PushNotificationSettingsError(state: PushNotificationSettingsUM.Error, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        UnableToLoadData(onRetryClick = state.onRetryClick)
    }
}