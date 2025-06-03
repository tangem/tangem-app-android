package com.tangem.feature.walletsettings.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetWithFooter
import com.tangem.core.ui.components.items.ItemWithIconAndSubtext
import com.tangem.core.ui.components.items.ItemWithIconAndSubtextShimmer
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.notifications.models.NotificationsEligibleNetwork
import com.tangem.feature.walletsettings.ui.state.NetworksAvailableForNotificationsUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun NetworksAvailableForNotificationsListBS(
    state: NetworksAvailableForNotificationsUM,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
) {
    TangemModalBottomSheetWithFooter<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        containerColor = TangemTheme.colors.background.tertiary,
        onBack = onBack,
        title = {
            TangemModalBottomSheetTitle(
                endIconRes = R.drawable.ic_close_24,
                onEndClick = onDismiss,
            )
        },
        content = {
            NetworksAvailableForNotificationsListBottomSheetContent(
                modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing16),
                state = state,
            )
        },
        footer = {
            SecondaryButton(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                text = stringResourceSafe(R.string.balance_hidden_got_it_button),
                onClick = onDismiss,
            )
        },
    )
}

@Composable
private fun NetworksAvailableForNotificationsListBottomSheetContent(
    state: NetworksAvailableForNotificationsUM,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.background(TangemTheme.colors.background.primary),
    ) {
        Header()
        SpacerH24()
        Networks(networks = state.networks, isLoading = state.isLoading)
    }
}

@Composable
private fun ColumnScope.Header() {
    Icon(
        painter = painterResource(id = R.drawable.ic_notification_48),
        contentDescription = null,
        tint = TangemTheme.colors.icon.accent,
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .size(TangemTheme.dimens.size56),
    )
    Text(
        text = stringResourceSafe(R.string.push_transactions_notifications_title),
        style = TangemTheme.typography.h2,
        color = TangemTheme.colors.text.primary1,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(
                start = TangemTheme.dimens.spacing34,
                end = TangemTheme.dimens.spacing34,
                top = TangemTheme.dimens.spacing28,
            ),
    )
    Text(
        text = stringResourceSafe(R.string.push_transactions_notifications_description),
        style = TangemTheme.typography.body2,
        color = TangemTheme.colors.text.secondary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(
                start = TangemTheme.dimens.spacing34,
                end = TangemTheme.dimens.spacing34,
                top = TangemTheme.dimens.spacing8,
            ),
    )
}

@Composable
private fun Networks(
    networks: ImmutableList<NotificationsEligibleNetwork>,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
) {
    Column(modifier = modifier) {
        Text(
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 12.dp, end = 12.dp),
            text = stringResourceSafe(R.string.common_supported_networks),
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.tertiary,
        )
        if (isLoading) {
            repeat(SHIMMERS_COUNT) {
                ItemWithIconAndSubtextShimmer()
            }
        } else {
            networks.fastForEach { network ->
                key(network.id) {
                    ItemWithIconAndSubtext(
                        icon = network.icon,
                        name = network.name,
                        symbol = network.symbol,
                    )
                }
            }
        }
    }
}

private const val SHIMMERS_COUNT = 10

@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_CustomTokenNetworkSelectorContent() {
    TangemThemePreview {
        NetworksAvailableForNotificationsListBS(
            state = NetworksAvailableForNotificationsUM(
                networks = persistentListOf(),
                isLoading = true,
            ),
            onDismiss = {},
            onBack = {},
        )
    }
}