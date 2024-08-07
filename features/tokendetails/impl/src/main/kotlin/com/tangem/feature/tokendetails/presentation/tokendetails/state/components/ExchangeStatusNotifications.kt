package com.tangem.feature.tokendetails.presentation.tokendetails.state.components

import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.notifications.CurrencyNotificationConfig
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.features.tokendetails.impl.R

@Immutable
internal sealed interface ExchangeStatusNotifications {

    sealed class CommonNotification(val config: NotificationConfig) : ExchangeStatusNotifications

    data class NeedVerification(val onGoToProviderClick: () -> Unit) : CommonNotification(
        config = NotificationConfig(
            title = resourceReference(R.string.express_exchange_notification_verification_title),
            subtitle = resourceReference(R.string.express_exchange_notification_verification_text),
            iconResId = R.drawable.ic_alert_triangle_20,
            buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                text = resourceReference(R.string.common_go_to_provider),
                onClick = onGoToProviderClick,
            ),
        ),
    )

    data class Failed(val onGoToProviderClick: () -> Unit) : CommonNotification(
        config = NotificationConfig(
            title = resourceReference(R.string.express_exchange_notification_failed_title),
            subtitle = resourceReference(R.string.express_exchange_notification_failed_text),
            iconResId = R.drawable.ic_alert_circle_24,
            buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                text = resourceReference(R.string.common_go_to_provider),
                onClick = onGoToProviderClick,
            ),
        ),
    )

    data class TokenRefunded(
        val cryptoCurrency: CryptoCurrency,
        val onReadMoreClick: () -> Unit,
        val onGoToTokenClick: () -> Unit,
    ) : ExchangeStatusNotifications {

        val config = CurrencyNotificationConfig(
            title = resourceReference(
                id = R.string.express_exchange_notification_refund_title,
                formatArgs = wrappedList(cryptoCurrency.symbol, cryptoCurrency.network.name),
            ),
            subtitle = CurrencyNotificationConfig.AnnotatedSubtitle(
                valueProvider = {
                    val linkText = stringResource(R.string.common_read_more)
                    val fullString = stringResource(
                        R.string.express_exchange_notification_refund_text,
                        cryptoCurrency.symbol,
                        linkText,
                    )

                    val linkTextPosition = fullString.length - linkText.length

                    buildAnnotatedString {
                        withStyle(SpanStyle(TangemTheme.colors.text.tertiary)) {
                            append(fullString.substring(0, linkTextPosition))
                        }

                        withStyle(SpanStyle(TangemTheme.colors.text.accent)) {
                            append(fullString.substring(linkTextPosition, fullString.length))
                        }
                    }
                },
                onClick = { value, position ->
                    val readMoreStyle = requireNotNull(value.spanStyles.getOrNull(1))
                    if (position in readMoreStyle.start..readMoreStyle.end) {
                        onReadMoreClick()
                    }
                },
            ),
            tokenIconState = CryptoCurrencyToIconStateConverter().convert(cryptoCurrency),
            buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                text = resourceReference(R.string.common_go_to_token),
                onClick = onGoToTokenClick,
            ),
        )
    }
}