package com.tangem.features.walletconnect.connections.ui

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.SecondaryButtonIconEnd
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.walletconnect.connections.components.AlertsComponent

@Composable
internal fun AlertsModalBottomSheet(config: TangemBottomSheetConfig) {
    TangemModalBottomSheet<AlertsComponent.AlertType>(
        config = config,
        title = { alert ->
            TangemModalBottomSheetTitle(
                endIconRes = R.drawable.ic_close_24,
                onEndClick = alert.onDismiss,
            )
        },
        content = { alert -> Content(alert = alert) },
    )
}

@Composable
private fun Content(alert: AlertsComponent.AlertType, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        ContentContainer(
            modifier = Modifier
                .heightIn(min = TangemTheme.dimens.size180)
                .fillMaxWidth()
                .padding(horizontal = TangemTheme.dimens.spacing16)
                .padding(bottom = 32.dp),
            alert = alert,
        )
        ButtonsContainer(modifier = Modifier.fillMaxWidth(), alert = alert)
    }
}

@Composable
private fun ContentContainer(alert: AlertsComponent.AlertType, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AlertIcon(alert = alert)
        AlertContentTitle(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = TangemTheme.dimens.spacing24),
            alert = alert,
        )
        AlertContentDescription(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = TangemTheme.dimens.spacing8),
            alert = alert,
        )
        AlertAudit(alert, modifier = Modifier.padding(top = TangemTheme.dimens.spacing16))
    }
}

@Suppress("LongMethod")
@Composable
private fun ButtonsContainer(alert: AlertsComponent.AlertType, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(all = TangemTheme.dimens.spacing16),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
    ) {
        val buttonsModifier = Modifier.fillMaxWidth()
        when (alert) {
            is AlertsComponent.AlertType.UnsafeDomain -> {
                PrimaryButton(
                    modifier = buttonsModifier,
                    text = stringResourceSafe(R.string.common_cancel),
                    onClick = alert.onDismiss,
                )
                SecondaryButton(
                    modifier = buttonsModifier,
                    onClick = alert.onConnect,
                    text = stringResourceSafe(R.string.wc_alert_connect_anyway),
                )
            }
            is AlertsComponent.AlertType.MaliciousTransaction -> {
                PrimaryButton(
                    modifier = buttonsModifier,
                    text = stringResourceSafe(R.string.common_cancel),
                    onClick = alert.onDismiss,
                )
                SecondaryButtonIconEnd(
                    modifier = buttonsModifier,
                    onClick = alert.onConnect,
                    iconResId = R.drawable.ic_tangem_24,
                    text = stringResourceSafe(R.string.wc_alert_connect_anyway),
                )
            }
            is AlertsComponent.AlertType.UnknownDomain -> {
                PrimaryButton(
                    modifier = buttonsModifier,
                    text = stringResourceSafe(R.string.common_cancel),
                    onClick = alert.onDismiss,
                )
                SecondaryButton(
                    modifier = buttonsModifier,
                    onClick = alert.onConnect,
                    text = stringResourceSafe(R.string.wc_alert_connect_anyway),
                )
            }
            is AlertsComponent.AlertType.VerifiedDomain -> SecondaryButton(
                modifier = buttonsModifier,
                text = stringResourceSafe(R.string.common_done),
                onClick = alert.onDismiss,
            )
            is AlertsComponent.AlertType.ConnectionTimeout -> {
                PrimaryButton(
                    modifier = buttonsModifier,
                    onClick = alert.onTryAgain,
                    text = stringResourceSafe(R.string.alert_button_try_again),
                )
                SecondaryButton(
                    modifier = buttonsModifier,
                    onClick = alert.onDismiss,
                    text = stringResourceSafe(R.string.balance_hidden_got_it_button),
                )
            }
            is AlertsComponent.AlertType.WrongCardSelected,
            is AlertsComponent.AlertType.UnknownError,
            is AlertsComponent.AlertType.UnsupportedMethod,
            -> SecondaryButton(
                modifier = buttonsModifier,
                onClick = alert.onDismiss,
                text = stringResourceSafe(R.string.balance_hidden_got_it_button),
            )
            is AlertsComponent.AlertType.WcDisconnected,
            is AlertsComponent.AlertType.UnsupportedNetworks,
            -> PrimaryButton(
                modifier = buttonsModifier,
                onClick = alert.onDismiss,
                text = stringResourceSafe(R.string.balance_hidden_got_it_button),
            )
        }
    }
}

@Composable
private fun AlertIcon(alert: AlertsComponent.AlertType, modifier: Modifier = Modifier) {
    val color = when (alert) {
        is AlertsComponent.AlertType.VerifiedDomain -> TangemTheme.colors.icon.accent
        is AlertsComponent.AlertType.WcDisconnected,
        is AlertsComponent.AlertType.ConnectionTimeout,
        is AlertsComponent.AlertType.UnsupportedNetworks,
        -> TangemTheme.colors.icon.informative
        is AlertsComponent.AlertType.WrongCardSelected,
        is AlertsComponent.AlertType.UnknownDomain,
        is AlertsComponent.AlertType.UnknownError,
        is AlertsComponent.AlertType.UnsupportedMethod,
        -> TangemTheme.colors.icon.attention
        is AlertsComponent.AlertType.MaliciousTransaction,
        is AlertsComponent.AlertType.UnsafeDomain,
        -> TangemTheme.colors.icon.warning
    }

    @DrawableRes val drawableId = when (alert) {
        is AlertsComponent.AlertType.WcDisconnected,
        is AlertsComponent.AlertType.ConnectionTimeout,
        -> R.drawable.ic_wallet_connect_24
        is AlertsComponent.AlertType.MaliciousTransaction -> R.drawable.img_knight_shield_32
        is AlertsComponent.AlertType.UnknownDomain,
        is AlertsComponent.AlertType.UnsafeDomain,
        -> R.drawable.img_knight_shield_32
        is AlertsComponent.AlertType.UnsupportedNetworks -> R.drawable.ic_network_new_24
        is AlertsComponent.AlertType.WrongCardSelected,
        is AlertsComponent.AlertType.UnknownError,
        is AlertsComponent.AlertType.UnsupportedMethod,
        -> R.drawable.img_attention_20
        is AlertsComponent.AlertType.VerifiedDomain -> R.drawable.img_approvale2_20
    }
    val iconTint = when (alert) {
        is AlertsComponent.AlertType.UnknownDomain,
        is AlertsComponent.AlertType.UnsafeDomain,
        is AlertsComponent.AlertType.MaliciousTransaction,
        is AlertsComponent.AlertType.UnsupportedNetworks,
        is AlertsComponent.AlertType.ConnectionTimeout,
        is AlertsComponent.AlertType.WcDisconnected,
        -> color
        is AlertsComponent.AlertType.VerifiedDomain,
        is AlertsComponent.AlertType.UnknownError,
        is AlertsComponent.AlertType.WrongCardSelected,
        is AlertsComponent.AlertType.UnsupportedMethod,
        -> Color.Unspecified
    }
    Box(
        modifier = modifier
            .size(TangemTheme.dimens.size56)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.1F)),
        contentAlignment = Alignment.Center,
        content = {
            Icon(
                modifier = Modifier.size(TangemTheme.dimens.size32),
                painter = painterResource(drawableId),
                contentDescription = null,
                tint = iconTint,
            )
        },
    )
}

@Composable
private fun AlertContentTitle(alert: AlertsComponent.AlertType, modifier: Modifier = Modifier) {
    @StringRes val titleRes: Int = when (alert) {
        is AlertsComponent.AlertType.UnknownDomain,
        is AlertsComponent.AlertType.UnsafeDomain,
        is AlertsComponent.AlertType.MaliciousTransaction,
        -> R.string.security_alert_title
        is AlertsComponent.AlertType.ConnectionTimeout -> R.string.wc_alert_connection_timeout_title
        is AlertsComponent.AlertType.UnknownError -> R.string.wc_alert_unknown_error_title
        is AlertsComponent.AlertType.UnsupportedNetworks -> R.string.wc_alert_unsupported_networks_title
        is AlertsComponent.AlertType.VerifiedDomain -> R.string.wc_alert_verified_domain_title
        is AlertsComponent.AlertType.WcDisconnected -> R.string.wc_alert_session_disconnected_title
        is AlertsComponent.AlertType.WrongCardSelected -> R.string.wc_alert_wrong_card_title
        is AlertsComponent.AlertType.UnsupportedMethod -> R.string.wc_alert_unsupported_method_title
    }
    Text(
        modifier = modifier,
        text = stringResourceSafe(titleRes),
        style = TangemTheme.typography.h3,
        color = TangemTheme.colors.text.primary1,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun AlertContentDescription(alert: AlertsComponent.AlertType, modifier: Modifier = Modifier) {
    val message = when (alert) {
        is AlertsComponent.AlertType.ConnectionTimeout -> stringResourceSafe(
            R.string.wc_alert_connection_timeout_description,
        )
        is AlertsComponent.AlertType.MaliciousTransaction -> alert.descriptionMessage
        is AlertsComponent.AlertType.UnknownDomain,
        is AlertsComponent.AlertType.UnsafeDomain,
        -> stringResourceSafe(R.string.wc_alert_domain_issues_description)
        is AlertsComponent.AlertType.UnknownError -> stringResourceSafe(
            R.string.wc_alert_unknown_error_description,
            alert.errorCode,
        )
        is AlertsComponent.AlertType.UnsupportedNetworks -> stringResourceSafe(
            R.string.wc_alert_unsupported_networks_description,
            alert.appName,
        )
        is AlertsComponent.AlertType.VerifiedDomain -> stringResourceSafe(
            R.string.wc_alert_verified_domain_description,
            alert.appName,
        )
        is AlertsComponent.AlertType.WcDisconnected -> stringResourceSafe(
            R.string.wc_alert_session_disconnected_description,
        )
        is AlertsComponent.AlertType.WrongCardSelected -> stringResourceSafe(R.string.wc_alert_wrong_card_description)
        is AlertsComponent.AlertType.UnsupportedMethod -> stringResourceSafe(
            R.string.wc_alert_unsupported_method_description,
        )
    }
    Text(
        modifier = modifier,
        text = message,
        style = TangemTheme.typography.body2,
        color = TangemTheme.colors.text.secondary,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun AlertAudit(alert: AlertsComponent.AlertType, modifier: Modifier = Modifier) {
    when (alert) {
        is AlertsComponent.AlertType.UnknownDomain -> Text(
            modifier = modifier
                .background(
                    shape = RoundedCornerShape(TangemTheme.dimens.radius16),
                    color = TangemTheme.colors.text.primary1.copy(alpha = 0.1F),
                )
                .padding(vertical = TangemTheme.dimens.spacing4, horizontal = TangemTheme.dimens.spacing12),
            text = stringResourceSafe(R.string.wc_alert_audit_unknown_domain),
            style = TangemTheme.typography.caption1,
            color = TangemTheme.colors.text.primary1,
        )
        is AlertsComponent.AlertType.UnsafeDomain -> Text(
            modifier = Modifier
                .padding(top = TangemTheme.dimens.spacing16)
                .background(
                    shape = RoundedCornerShape(TangemTheme.dimens.radius16),
                    color = TangemTheme.colors.text.warning.copy(alpha = 0.1F),
                )
                .padding(vertical = TangemTheme.dimens.spacing4, horizontal = TangemTheme.dimens.spacing12),
            text = stringResourceSafe(R.string.wc_alert_audit_unknown_domain),
            style = TangemTheme.typography.caption1,
            color = TangemTheme.colors.text.warning,
        )
        is AlertsComponent.AlertType.ConnectionTimeout,
        is AlertsComponent.AlertType.MaliciousTransaction,
        is AlertsComponent.AlertType.UnknownError,
        is AlertsComponent.AlertType.UnsupportedNetworks,
        is AlertsComponent.AlertType.VerifiedDomain,
        is AlertsComponent.AlertType.WcDisconnected,
        is AlertsComponent.AlertType.WrongCardSelected,
        is AlertsComponent.AlertType.UnsupportedMethod,
        -> Unit
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AlertsModalBottomSheet_Preview(
    @PreviewParameter(AlertTypesProvider::class) alert: AlertsComponent.AlertType,
) {
    TangemThemePreview {
        AlertsModalBottomSheet(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = {},
                content = alert,
            ),
        )
    }
}

private class AlertTypesProvider : CollectionPreviewParameterProvider<AlertsComponent.AlertType>(
    collection = listOf(
        AlertsComponent.AlertType.VerifiedDomain(appName = "React App", onDismiss = {}),
        AlertsComponent.AlertType.UnknownDomain(onConnect = {}, onDismiss = {}),
        AlertsComponent.AlertType.UnsafeDomain(onConnect = {}, onDismiss = {}),
        AlertsComponent.AlertType.MaliciousTransaction(
            descriptionMessage = "The transaction approves erc20 tokens to a known malicious address",
            onConnect = {},
            onDismiss = {},
        ),
        AlertsComponent.AlertType.UnsupportedNetworks(appName = "React App", onDismiss = {}),
        AlertsComponent.AlertType.WcDisconnected(onDismiss = {}),
        AlertsComponent.AlertType.UnknownError(errorCode = 8005, onDismiss = {}),
        AlertsComponent.AlertType.WrongCardSelected(onDismiss = {}),
        AlertsComponent.AlertType.ConnectionTimeout(onTryAgain = {}, onDismiss = {}),
        AlertsComponent.AlertType.UnsupportedMethod(onDismiss = {}),
    ),
)