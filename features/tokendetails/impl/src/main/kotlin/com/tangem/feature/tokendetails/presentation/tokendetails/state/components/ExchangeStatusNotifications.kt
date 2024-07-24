package com.tangem.feature.tokendetails.presentation.tokendetails.state.components

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.features.tokendetails.impl.R

@Immutable
internal sealed class ExchangeStatusNotifications(val config: NotificationConfig) {

    data class NeedVerification(
        val onGoToProviderClick: () -> Unit,
    ) : ExchangeStatusNotifications(
        config = NotificationConfig(
            title = TextReference.Res(R.string.express_exchange_notification_verification_title),
            subtitle = TextReference.Res(R.string.express_exchange_notification_verification_text),
            iconResId = R.drawable.ic_alert_triangle_20,
            buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                text = TextReference.Res(R.string.common_go_to_provider),
                onClick = onGoToProviderClick,
            ),
        ),
    )

    data class Failed(
        val onGoToProviderClick: () -> Unit,
    ) : ExchangeStatusNotifications(
        config = NotificationConfig(
            title = TextReference.Res(R.string.express_exchange_notification_failed_title),
            subtitle = TextReference.Res(R.string.express_exchange_notification_failed_text),
            iconResId = R.drawable.ic_alert_circle_24,
            buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                text = TextReference.Res(R.string.common_go_to_provider),
                onClick = onGoToProviderClick,
            ),
        ),
    )

    data class TokenRefunded(
        val cryptoCurrency: CryptoCurrency,
        val onGoToTokenClick: () -> Unit,
    ) : ExchangeStatusNotifications(
        config = NotificationConfig(
            title = stringReference("TITLE FOR TOKEN REFUND"),
            subtitle = stringReference("SUBTITLE FOR TOKEN REFUND"),
            iconResId = R.drawable.ic_alert_triangle_20,
            buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                text = stringReference("Go to token"),
                onClick = onGoToTokenClick,
            ),
        ),
    )
}
