package com.tangem.features.pushnotificationsettings.impl.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.pushnotificationsettings.impl.R
import com.tangem.features.pushnotificationsettings.impl.entity.PushNotificationSettingsUM

@Composable
internal fun PushNotificationSettingsScreen(
    state: PushNotificationSettingsUM,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = TangemTheme.colors.background.secondary,
        topBar = {
            TangemTopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = resourceReference(R.string.push_notification_settings_title),
                startButton = TopAppBarButtonUM.Back(onBackClicked = onBackClick),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (state) {
                is PushNotificationSettingsUM.Loading -> PushNotificationSettingsLoading()
                is PushNotificationSettingsUM.Content -> PushNotificationSettingsContent(state = state)
                is PushNotificationSettingsUM.Error -> PushNotificationSettingsError(state = state)
            }
        }
    }
}