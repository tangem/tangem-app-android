package com.tangem.features.pushnotificationsettings.impl.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.pushnotificationsettings.impl.entity.AllowPushNotificationsBannerUM
import com.tangem.features.pushnotificationsettings.impl.entity.PushNotificationSettingsUM
import com.tangem.features.pushnotificationsettings.impl.entity.ToggleId

@Composable
internal fun PushNotificationSettingsContent(
    state: PushNotificationSettingsUM.Content,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(
            horizontal = TangemTheme.dimens.spacing16,
            vertical = TangemTheme.dimens.spacing12,
        ),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing16),
    ) {
        item(key = "banner") {
            AnimatedBannerSlot(banner = state.banner)
        }

        items(items = state.toggles, key = { it.id.name }) { toggle ->
            NotificationSettingRow(
                modifier = Modifier.animateItem(),
                toggle = toggle,
                showInlineMoreInfoLink = toggle.id == ToggleId.TransactionAlerts,
                onMoreInfoClick = state.onMoreInfoClick,
            )
        }
    }
}

@Composable
private fun AnimatedBannerSlot(banner: AllowPushNotificationsBannerUM?) {
    var lastVisible by remember { mutableStateOf(banner) }
    if (banner != null) {
        lastVisible = banner
    }
    AnimatedVisibility(
        visible = banner != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        lastVisible?.let { AllowPushNotificationsBanner(state = it) }
    }
}