package com.tangem.features.account.details.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.R
import com.tangem.common.ui.account.AccountIconPreviewData
import com.tangem.common.ui.account.AccountRow
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.components.buttons.SecondarySmallButton
import com.tangem.core.ui.components.buttons.SmallButtonConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.account.details.entity.AccountDetailsUM

@Composable
internal fun AccountDetailsContent(state: AccountDetailsUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(color = TangemTheme.colors.background.secondary)
            .fillMaxSize()
            .imePadding()
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppBarWithBackButton(
            onBackClick = state.onCloseClick,
            modifier = Modifier.height(TangemTheme.dimens.size56),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = TangemTheme.dimens.spacing16)
                .weight(1f),

        ) {
            Text(
                modifier = Modifier.padding(top = TangemTheme.dimens.spacing12),
                text = stringResourceSafe(R.string.account_details_title),
                style = TangemTheme.typography.h1,
                color = TangemTheme.colors.text.primary1,
            )
            SpacerH16()
            AccountRow(state)
            SpacerH16()
            ManageTokensRow(state)
            when (state.archiveMode) {
                is AccountDetailsUM.ArchiveMode.Available -> {
                    SpacerH16()
                    ArchiveAccountRow(state.archiveMode)
                    SpacerH(8.dp)
                    Text(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        text = stringResourceSafe(R.string.account_details_archive_description),
                        style = TangemTheme.typography.caption2,
                        color = TangemTheme.colors.text.tertiary,
                    )
                }
                AccountDetailsUM.ArchiveMode.None -> Unit
            }
        }
    }
}

@Composable
private fun ArchiveAccountRow(state: AccountDetailsUM.ArchiveMode.Available) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TangemTheme.dimens.radius12))
            .background(TangemTheme.colors.background.primary)
            .clickable(onClick = state.onArchiveAccountClick)
            .padding(all = TangemTheme.dimens.spacing12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        Icon(
            tint = TangemTheme.colors.icon.warning,
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_archive_24),
            contentDescription = null,
        )
        Text(
            text = stringResourceSafe(R.string.account_details_archive),
            color = TangemTheme.colors.text.warning,
            style = TangemTheme.typography.subtitle1,
        )
    }
}

@Composable
private fun ManageTokensRow(state: AccountDetailsUM) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TangemTheme.dimens.radius12))
            .background(TangemTheme.colors.background.primary)
            .clickable(onClick = state.onManageTokensClick)
            .padding(all = TangemTheme.dimens.spacing12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_group_24),
            tint = TangemTheme.colors.icon.secondary,
            contentDescription = null,
        )
        Text(
            text = stringResourceSafe(R.string.main_manage_tokens),
            color = TangemTheme.colors.text.primary1,
            style = TangemTheme.typography.subtitle1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AccountRow(state: AccountDetailsUM) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TangemTheme.dimens.radius12))
            .background(TangemTheme.colors.background.primary)
            .clickable(onClick = state.onAccountEditClick)
            .padding(all = TangemTheme.dimens.spacing12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        AccountRow(
            title = state.accountName,
            subtitle = resourceReference(R.string.account_form_name),
            icon = state.accountIcon,
            modifier = Modifier.weight(1f),
            isReverse = true,
        )

        SecondarySmallButton(
            config = SmallButtonConfig(
                text = resourceReference(R.string.common_edit),
                onClick = state.onAccountEditClick,
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WcConnectionsContentPreview(@PreviewParameter(PreviewStateProvider::class) params: AccountDetailsUM) {
    TangemThemePreview {
        AccountDetailsContent(state = params)
    }
}

private class PreviewStateProvider : CollectionPreviewParameterProvider<AccountDetailsUM>(
    buildList {
        val accountName = "Main"
        var portfolioIcon = AccountIconPreviewData.randomAccountIcon()
        val first = AccountDetailsUM(
            onCloseClick = {},
            onAccountEditClick = {},
            onManageTokensClick = {},
            archiveMode = AccountDetailsUM.ArchiveMode.Available(
                onArchiveAccountClick = {},
            ),
            accountName = stringReference(accountName),
            accountIcon = portfolioIcon,
        )
        add(first)
        portfolioIcon = AccountIconPreviewData.randomAccountIcon(letter = true)
        add(first.copy(accountIcon = portfolioIcon))
        add(first.copy(archiveMode = AccountDetailsUM.ArchiveMode.None))
    },
)