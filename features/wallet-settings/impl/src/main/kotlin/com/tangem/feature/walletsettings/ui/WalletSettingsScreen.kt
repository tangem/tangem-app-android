package com.tangem.feature.walletsettings.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.TangemSwitch
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.block.BlockCard
import com.tangem.core.ui.components.block.BlockItem
import com.tangem.core.ui.components.items.DescriptionItem
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.TestTags
import com.tangem.core.ui.utils.requestPushPermission
import com.tangem.feature.walletsettings.component.preview.PreviewWalletSettingsComponent
import com.tangem.feature.walletsettings.entity.WalletSettingsItemUM
import com.tangem.feature.walletsettings.entity.WalletSettingsUM
import com.tangem.feature.walletsettings.impl.R
import com.tangem.features.pushnotifications.api.utils.getPushPermissionOrNull

@Composable
internal fun WalletSettingsScreen(
    state: WalletSettingsUM,
    dialog: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = TangemTheme.colors.background.secondary

    Scaffold(
        modifier = modifier,
        containerColor = backgroundColor,
        topBar = {
            TangemTopAppBar(
                modifier = Modifier.statusBarsPadding(),
                startButton = TopAppBarButtonUM.Back(state.popBack),
            )
        },
        content = { paddingValues ->
            Content(
                modifier = Modifier
                    .padding(paddingValues)
                    .testTag(TestTags.WALLET_SETTINGS_SCREEN),
                state = state,
            )

            dialog()
        },
    )
}

@Composable
private fun Content(state: WalletSettingsUM, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing16),
        contentPadding = PaddingValues(
            top = TangemTheme.dimens.spacing16,
            bottom = TangemTheme.dimens.spacing16,
        ),
    ) {
        item {
            Text(
                modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing16),
                text = stringResourceSafe(id = R.string.wallet_settings_title),
                style = TangemTheme.typography.h1,
                color = TangemTheme.colors.text.primary1,
            )
        }
        items(
            items = state.items,
            key = WalletSettingsItemUM::id,
        ) { item ->
            val itemModifier = Modifier
                .padding(horizontal = TangemTheme.dimens.spacing16)
                .fillMaxWidth()
                .testTag(TestTags.WALLET_SETTINGS_SCREEN_ITEM)

            when (item) {
                is WalletSettingsItemUM.WithItems -> ItemsBlock(
                    modifier = itemModifier,
                    model = item,
                )
                is WalletSettingsItemUM.WithText -> TextBlock(
                    modifier = itemModifier,
                    model = item,
                )
                is WalletSettingsItemUM.WithSwitch -> SwitchBlock(
                    modifier = itemModifier,
                    model = item,
                )
                is WalletSettingsItemUM.DescriptionWithMore -> DescriptionWithMoreBlock(
                    modifier = itemModifier,
                    model = item,
                    yOffset = DESCRIPTION_OFFSET,
                    onDescriptionClick = item.onClick,
                )
                is WalletSettingsItemUM.NotificationPermission -> NotificationAlertBlock(
                    modifier = itemModifier,
                    model = item,
                )
            }
        }
    }

    val requestPushPermission = requestPushPermission(
        onAllow = { state.onPushNotificationPermissionGranted(true) },
        onDeny = { state.onPushNotificationPermissionGranted(false) },
        pushPermission = getPushPermissionOrNull(),
    )

    if (state.requestPushNotificationsPermission) {
        requestPushPermission()
    }
}

@Composable
private fun ItemsBlock(model: WalletSettingsItemUM.WithItems, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    shape = TangemTheme.shapes.roundedCornersXMedium,
                    color = TangemTheme.colors.background.primary,
                ),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top,
        ) {
            model.blocks.forEach { block ->
                BlockItem(
                    modifier = Modifier.fillMaxWidth(),
                    model = block,
                )
            }
        }

        Text(
            modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing12),
            text = model.description.resolveReference(),
            color = TangemTheme.colors.text.tertiary,
            style = TangemTheme.typography.caption2,
        )
    }
}

@Composable
private fun TextBlock(model: WalletSettingsItemUM.WithText, modifier: Modifier = Modifier) {
    BlockCard(
        modifier = modifier.fillMaxWidth(),
        enabled = model.isEnabled,
        onClick = model.onClick,
    ) {
        Column(
            modifier = Modifier.padding(all = TangemTheme.dimens.spacing12),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
        ) {
            Text(
                text = model.title.resolveReference(),
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.subtitle2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = model.text.resolveReference(),
                color = TangemTheme.colors.text.primary1,
                style = TangemTheme.typography.body1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SwitchBlock(model: WalletSettingsItemUM.WithSwitch, modifier: Modifier = Modifier) {
    BlockCard(
        modifier = modifier.fillMaxWidth(),
        enabled = model.isChecked,
    ) {
        Row(
            modifier = Modifier.padding(all = TangemTheme.dimens.spacing12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12, Alignment.Start),
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = model.title.resolveReference(),
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.primary1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            TangemSwitch(
                checked = model.isChecked,
                onCheckedChange = model.onCheckedChange,
            )
        }
    }
}

@Composable
private fun NotificationAlertBlock(model: WalletSettingsItemUM.NotificationPermission, modifier: Modifier = Modifier) {
    Notification(
        config = NotificationConfig(
            title = model.title,
            subtitle = model.description,
            iconResId = R.drawable.ic_alert_triangle_20,
        ),
        modifier = modifier,
    )
}

@Composable
private fun DescriptionWithMoreBlock(
    model: WalletSettingsItemUM.DescriptionWithMore,
    onDescriptionClick: () -> Unit,
    modifier: Modifier = Modifier,
    yOffset: Dp = 0.dp,
) {
    DescriptionItem(
        modifier = modifier
            .offset(y = yOffset)
            .padding(horizontal = 12.dp),
        description = model.text,
        hasFullDescription = true,
        textStyle = TangemTheme.typography.caption2,
        onReadMoreClick = onDescriptionClick,
    )
}

private val DESCRIPTION_OFFSET = -8.dp

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_WalletSettingsScreen() {
    TangemThemePreview {
        PreviewWalletSettingsComponent().Content(modifier = Modifier.fillMaxSize())
    }
}
// endregion Preview