package com.tangem.common.ui.notifications

import com.tangem.common.ui.R
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.resourceReference

object ExpressNotificationsUM {

    data class NeedVerification(val onGoToProviderClick: () -> Unit) : NotificationUM.Warning(
        title = resourceReference(R.string.express_exchange_notification_verification_title),
        subtitle = resourceReference(R.string.express_exchange_notification_verification_text),
        iconResId = R.drawable.ic_alert_triangle_20,
        buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
            text = resourceReference(R.string.common_go_to_provider),
            onClick = onGoToProviderClick,
        ),
    )

    data class FailedByProvider(val onGoToProviderClick: () -> Unit) : NotificationUM.Error(
        title = resourceReference(R.string.express_exchange_notification_failed_title),
        subtitle = resourceReference(R.string.express_exchange_notification_failed_text),
        iconResId = R.drawable.ic_alert_circle_24,
        buttonState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
            text = resourceReference(R.string.common_go_to_provider),
            onClick = onGoToProviderClick,
        ),
    )
}