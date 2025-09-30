package com.tangem.feature.walletsettings.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.account.AccountRow
import com.tangem.common.ui.userwallet.CardImage
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerH8
import com.tangem.core.ui.components.TangemSwitch
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.block.BlockCard
import com.tangem.core.ui.components.block.BlockItem
import com.tangem.core.ui.components.buttons.SecondarySmallButton
import com.tangem.core.ui.components.buttons.SmallButtonConfig
import com.tangem.core.ui.components.items.DescriptionItem
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.WalletSettingsScreenTestTags
import com.tangem.feature.walletsettings.component.preview.PreviewWalletSettingsComponent
import com.tangem.feature.walletsettings.entity.WalletSettingsAccountsUM
import com.tangem.feature.walletsettings.entity.WalletSettingsItemUM
import com.tangem.feature.walletsettings.entity.WalletSettingsUM
import com.tangem.feature.walletsettings.impl.R

@Composable
internal fun WalletSettingsScreen(
    state: WalletSettingsUM,
    modifier: Modifier = Modifier,
    dialog: @Composable () -> Unit,
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
                    .testTag(WalletSettingsScreenTestTags.SCREEN_CONTAINER),
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
            val offsetModifier = when (item) {
                is WalletSettingsAccountsUM.Account,
                is WalletSettingsAccountsUM.Footer,
                -> Modifier.padding(horizontal = TangemTheme.dimens.spacing16)
                else -> Modifier.padding(
                    start = TangemTheme.dimens.spacing16,
                    end = TangemTheme.dimens.spacing16,
                    top = TangemTheme.dimens.spacing16,
                )
            }
            val itemModifier = offsetModifier
                .fillMaxWidth()
                .testTag(WalletSettingsScreenTestTags.SCREEN_ITEM)

            when (item) {
                is WalletSettingsItemUM.WithItems -> ItemsBlock(
                    modifier = itemModifier,
                    model = item,
                )
                is WalletSettingsItemUM.CardBlock -> CardBlock(
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
                is WalletSettingsItemUM.UpgradeWallet -> UpgradeWalletBlock(
                    modifier = itemModifier,
                    model = item,
                )
                is WalletSettingsAccountsUM.Header -> AccountsHeader(item, itemModifier)
                is WalletSettingsAccountsUM.Account -> AccountItem(item, itemModifier)
                is WalletSettingsAccountsUM.Footer -> AccountsFooter(item, itemModifier)
            }
        }
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
private fun CardBlock(model: WalletSettingsItemUM.CardBlock, modifier: Modifier = Modifier) {
    BlockCard(
        modifier = modifier.fillMaxWidth(),
        enabled = model.isEnabled,
        onClick = model.onClick,
    ) {
        Row(
            modifier = Modifier.padding(all = TangemTheme.dimens.spacing12),
            horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CardImage(model.imageState)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = model.title.resolveReference(),
                    color = TangemTheme.colors.text.tertiary,
                    style = TangemTheme.typography.caption2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = model.text.resolveReference(),
                    color = TangemTheme.colors.text.primary1,
                    style = TangemTheme.typography.subtitle1,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            }
            SecondarySmallButton(
                config = SmallButtonConfig(
                    enabled = model.isEnabled,
                    text = resourceReference(R.string.common_rename),
                    onClick = model.onClick,
                ),
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
private fun UpgradeWalletBlock(model: WalletSettingsItemUM.UpgradeWallet, modifier: Modifier = Modifier) {
    Notification(
        config = NotificationConfig(
            title = model.title,
            subtitle = model.description,
            iconResId = R.drawable.ic_hardware_backup_36,
            iconSize = 36.dp,
            onClick = model.onClick,
            onCloseClick = model.onDismissClick,
        ),
        modifier = modifier,
    )
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
private fun AccountsHeader(model: WalletSettingsAccountsUM.Header, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier
            .background(
                shape = RoundedCornerShape(
                    topStart = TangemTheme.dimens.radius16,
                    topEnd = TangemTheme.dimens.radius16,
                ),
                color = TangemTheme.colors.background.primary,
            )
            .padding(
                start = TangemTheme.dimens.spacing12,
                end = TangemTheme.dimens.spacing12,
                top = TangemTheme.dimens.spacing8,
                bottom = TangemTheme.dimens.spacing4,
            ),
        text = model.text.resolveReference(),
        style = TangemTheme.typography.subtitle2,
        color = TangemTheme.colors.text.tertiary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun AccountItem(model: WalletSettingsAccountsUM.Account, modifier: Modifier = Modifier) {
    val subtitle = stringResourceSafe(
        id = R.string.account_label_tokens_info,
        formatArgs = arrayOf(
            model.tokensInfo.resolveReference(),
            model.networksInfo.resolveReference(),
        ),
    )
    AccountRow(
        modifier = modifier
            .background(color = TangemTheme.colors.background.primary)
            .clickable(onClick = model.onClick)
            .padding(12.dp),
        title = model.accountName,
        subtitle = stringReference(subtitle),
        icon = model.accountIconUM,
    )
}

@Composable
private fun AccountsFooter(model: WalletSettingsAccountsUM.Footer, modifier: Modifier = Modifier) {
    Column(modifier) {
        Column(
            modifier = Modifier.background(
                shape = RoundedCornerShape(
                    bottomStart = TangemTheme.dimens.radius16,
                    bottomEnd = TangemTheme.dimens.radius16,
                ),
                color = TangemTheme.colors.background.primary,
            ),
        ) {
            AddAccountRow(model.addAccount)
            SpacerH(
                height = TangemTheme.dimens.size0_5,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TangemTheme.dimens.spacing12)
                    .background(TangemTheme.colors.stroke.primary),
            )
            BlockItem(
                modifier = Modifier.fillMaxWidth(),
                model = model.archivedAccounts,
            )
        }
        SpacerH8()
        Text(
            modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing12),
            text = model.description.resolveReference(),
            color = TangemTheme.colors.text.tertiary,
            style = TangemTheme.typography.caption2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AddAccountRow(model: WalletSettingsAccountsUM.Footer.AddAccountUM, modifier: Modifier = Modifier) {
    val iconTint: Color
    val backgroundColor: Color
    val textColor: Color
    if (model.addAccountEnabled) {
        iconTint = TangemTheme.colors.icon.accent
        backgroundColor = TangemTheme.colors.icon.accent.copy(alpha = 0.1f)
        textColor = TangemTheme.colors.text.accent
    } else {
        iconTint = TangemTheme.colors.icon.inactive
        backgroundColor = TangemTheme.colors.field.primary
        textColor = TangemTheme.colors.text.disabled
    }
    Row(
        modifier = modifier
            .clickable(onClick = model.onAddAccountClick)
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(backgroundColor)
                .clickable(onClick = { model.onAddAccountClick() }),
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                tint = iconTint,
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_plus_24),
                contentDescription = null,
            )
        }

        Text(
            text = model.title.resolveReference(),
            color = textColor,
            style = TangemTheme.typography.subtitle1,
        )
    }
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

@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_WalletSettingsScreen1() {
    TangemThemePreview {
        UpgradeWalletBlock(
            model = WalletSettingsItemUM.UpgradeWallet(
                id = "upgrade_wallet",
                title = stringReference("Upgrade wallet with a hardware backup"),
                description = stringReference("Keep your crypto safe with Tangem’s best-in-class hardware wallet."),
                onClick = {},
                onDismissClick = {},
            ),
        )
    }
}
// endregion Preview