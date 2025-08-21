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
import com.tangem.common.ui.account.getResId
import com.tangem.common.ui.account.getUiColor
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.components.buttons.SecondarySmallButton
import com.tangem.core.ui.components.buttons.SmallButtonConfig
import com.tangem.core.ui.components.fields.AutoSizeTextField
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.account.common.CryptoPortfolioIconUM
import com.tangem.features.account.common.toUM
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
            SpacerH16()
            ArchiveAccountRow(state)
            SpacerH(8.dp)
            Text(
                modifier = Modifier.padding(horizontal = 12.dp),
                text = stringResourceSafe(R.string.account_details_archive_description),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
            )
        }
    }
}

@Composable
private fun ArchiveAccountRow(state: AccountDetailsUM) {
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
        AccountIcon(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(9.dp)),
            accountName = state.accountName,
            accountIcon = state.accountIcon,
        )
        Column(
            modifier = Modifier
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing2),
        ) {
            Text(
                text = stringResourceSafe(R.string.account_form_name),
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.caption2,
            )
            AutoSizeTextField(
                textStyle = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.primary1,
                value = state.accountName,
                singleLine = true,
                readOnly = true,
                onValueChange = {},
            )
        }

        SecondarySmallButton(
            config = SmallButtonConfig(
                text = resourceReference(R.string.common_edit),
                onClick = state.onAccountEditClick,
            ),
        )
    }
}

// todo account make reusable
@Composable
internal fun AccountIcon(accountName: String, accountIcon: CryptoPortfolioIconUM, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.background(accountIcon.color.getUiColor()),
    ) {
        val icon = accountIcon.value
        val letter = accountName.first()
        when {
            icon == CryptoPortfolioIcon.Icon.Letter -> Text(
                text = letter.uppercase(),
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.constantWhite,
            )
            else -> Icon(
                modifier = Modifier.size(20.dp),
                tint = TangemTheme.colors.text.constantWhite,
                imageVector = ImageVector.vectorResource(id = icon.getResId()),
                contentDescription = null,
            )
        }
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
        var portfolioIcon = CryptoPortfolioIcon.ofDefaultCustomAccount().toUM()
        val first = AccountDetailsUM(
            onCloseClick = {},
            onAccountEditClick = {},
            onManageTokensClick = {},
            onArchiveAccountClick = {},
            accountName = "Main",
            accountIcon = portfolioIcon,
        )
        add(first)
        portfolioIcon = portfolioIcon.copy(
            value = CryptoPortfolioIcon.Icon.Letter,
            color = CryptoPortfolioIcon.Color.entries.random(),
        )
        add(first.copy(accountIcon = portfolioIcon))
    },
)