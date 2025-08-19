package com.tangem.features.walletconnect.connections.ui

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
            is AlertsComponent.AlertType.UnsupportedMethod,
            is AlertsComponent.AlertType.RequiredAddNetwork,
            is AlertsComponent.AlertType.RequiredReconnectWithNetwork,
            is AlertsComponent.AlertType.TangemUnsupportedNetwork,
            -> SecondaryButton(
                modifier = buttonsModifier,
                onClick = alert.onDismiss,
                text = stringResourceSafe(R.string.balance_hidden_got_it_button),
            )
            is AlertsComponent.AlertType.WcDisconnected -> PrimaryButton(
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
        is AlertsComponent.AlertType.WcDisconnected -> TangemTheme.colors.icon.informative
        is AlertsComponent.AlertType.UnsupportedMethod,
        is AlertsComponent.AlertType.RequiredAddNetwork,
        is AlertsComponent.AlertType.RequiredReconnectWithNetwork,
        is AlertsComponent.AlertType.TangemUnsupportedNetwork,
        -> TangemTheme.colors.icon.attention
    }

    @DrawableRes val drawableId = when (alert) {
        is AlertsComponent.AlertType.WcDisconnected -> R.drawable.ic_wallet_connect_24
        is AlertsComponent.AlertType.UnsupportedMethod,
        is AlertsComponent.AlertType.RequiredAddNetwork,
        is AlertsComponent.AlertType.RequiredReconnectWithNetwork,
        is AlertsComponent.AlertType.TangemUnsupportedNetwork,
        -> R.drawable.img_attention_20
    }
    val iconTint = when (alert) {
        is AlertsComponent.AlertType.WcDisconnected -> color
        is AlertsComponent.AlertType.UnsupportedMethod,
        is AlertsComponent.AlertType.RequiredAddNetwork,
        is AlertsComponent.AlertType.RequiredReconnectWithNetwork,
        is AlertsComponent.AlertType.TangemUnsupportedNetwork,
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
        is AlertsComponent.AlertType.WcDisconnected -> R.string.wc_alert_session_disconnected_title
        is AlertsComponent.AlertType.UnsupportedMethod -> R.string.wc_alert_unsupported_method_title
        is AlertsComponent.AlertType.RequiredAddNetwork -> R.string.wc_alert_add_network_to_portfolio_title
        is AlertsComponent.AlertType.RequiredReconnectWithNetwork -> R.string.wc_alert_network_not_connected_title
        is AlertsComponent.AlertType.TangemUnsupportedNetwork -> R.string.wc_alert_unsupported_network_title
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
        is AlertsComponent.AlertType.WcDisconnected -> stringResourceSafe(
            R.string.wc_alert_session_disconnected_description,
        )
        is AlertsComponent.AlertType.UnsupportedMethod -> stringResourceSafe(
            R.string.wc_alert_unsupported_method_description,
        )
        is AlertsComponent.AlertType.RequiredAddNetwork -> stringResourceSafe(
            R.string.wc_alert_unsupported_method_description,
            alert.network,
        )
        is AlertsComponent.AlertType.RequiredReconnectWithNetwork -> stringResourceSafe(
            R.string.wc_alert_network_not_connected_description,
            alert.network,
        )
        is AlertsComponent.AlertType.TangemUnsupportedNetwork -> stringResourceSafe(
            R.string.wc_alert_unsupported_network_description,
            alert.network,
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
        AlertsComponent.AlertType.WcDisconnected(onDismiss = {}),
        AlertsComponent.AlertType.UnsupportedMethod(onDismiss = {}),
        AlertsComponent.AlertType.TangemUnsupportedNetwork(network = "Solana", onDismiss = {}),
        AlertsComponent.AlertType.RequiredAddNetwork(network = "Solana", onDismiss = {}),
        AlertsComponent.AlertType.RequiredReconnectWithNetwork(network = "Solana", onDismiss = {}),
    ),
)