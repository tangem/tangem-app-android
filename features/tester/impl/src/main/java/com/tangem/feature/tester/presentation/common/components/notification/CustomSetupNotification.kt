package com.tangem.feature.tester.presentation.common.components.notification

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.feature.tester.impl.R

@Composable
internal fun CustomSetupNotification(subtitle: TextReference, modifier: Modifier = Modifier) {
    Notification(
        config = NotificationConfig(
            subtitle = subtitle,
            iconResId = R.drawable.ic_alert_triangle_20,
            title = resourceReference(id = R.string.custom_setup_warning_title),
        ),
        modifier = modifier,
    )
}