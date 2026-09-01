package com.tangem.features.account.details.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.R
import com.tangem.common.ui.account.AccountIconPreviewData
import com.tangem.common.ui.account.AccountRowLegacy
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.components.buttons.SecondarySmallButton
import com.tangem.core.ui.components.buttons.SmallButtonConfig
import com.tangem.core.ui.components.inputrow.inner.DividerContainer
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.accounts.AccountDetailsScreenTestTags
import com.tangem.features.account.details.entity.AccountDetailsUM

private const val NETWORKS_INFO_LINK_TAG = "networks_info_more_info"

@Composable
internal fun AccountDetailsContent(state: AccountDetailsUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(color = TangemTheme.colors3.bg.primary)
            .fillMaxSize()
            .imePadding()
            .systemBarsPadding()
            .testTag(AccountDetailsScreenTestTags.ACCOUNT_DETAILS_CONTAINER),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppBarWithBackButton(
            onBackClick = state.onCloseClick,
            modifier = Modifier.height(TangemTheme.dimens.size56),
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = TangemTheme.dimens.spacing16)
                .weight(1f),

        ) {
            Text(
                modifier = Modifier.padding(top = TangemTheme.dimens.spacing12),
                text = stringResourceSafe(R.string.account_details_title),
                style = TangemTheme.typography3.heading.medium,
                color = TangemTheme.colors3.text.primary,
            )
            AccountRow(state)
            if (state.members != null || state.isManageTokensAvailable) {
                AccountActionsBlock(state)
            }
            state.networksInfo?.let { NetworksInfoText(it) }
            when (state.archiveMode) {
                is AccountDetailsUM.ArchiveMode.Available -> {
                    Column {
                        ArchiveAccountRow(state.archiveMode)
                        SpacerH(8.dp)
                        Text(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            text = stringResourceSafe(R.string.account_details_archive_description),
                            style = TangemTheme.typography3.caption.medium,
                            color = TangemTheme.colors3.text.tertiary,
                        )
                    }
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
            .background(TangemTheme.colors3.bg.secondary)
            .clickable(enabled = !state.isLoading, onClick = state.onArchiveAccountClick)
            .padding(all = TangemTheme.dimens.spacing12)
            .testTag(AccountDetailsScreenTestTags.ARCHIVE_ACCOUNT_BUTTON),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(
                color = TangemTheme.colors3.text.tertiary,
                modifier = Modifier.size(TangemTheme.dimens.size24),
            )
            Text(
                text = stringResourceSafe(R.string.account_details_archiving),
                color = TangemTheme.colors3.text.tertiary,
                style = TangemTheme.typography3.body.medium,
            )
        } else {
            Icon(
                tint = TangemTheme.colors3.icon.accent.red,
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_archive_24),
                contentDescription = null,
            )
            Text(
                text = stringResourceSafe(R.string.account_details_archive),
                color = TangemTheme.colors3.text.status.error,
                style = TangemTheme.typography3.body.medium,
            )
        }
    }
}

@Composable
private fun AccountActionsBlock(state: AccountDetailsUM) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TangemTheme.dimens.radius12))
            .background(TangemTheme.colors3.bg.secondary),
    ) {
        val members = state.members
        if (members != null) {
            DividerContainer(showDivider = state.isManageTokensAvailable) {
                MembersRow(members)
            }
        }
        if (state.isManageTokensAvailable) {
            ManageTokensRow(state)
        }
    }
}

@Composable
private fun MembersRow(state: AccountDetailsUM.MembersRowUM) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = state.onClick)
            .padding(all = TangemTheme.dimens.spacing12)
            .testTag(AccountDetailsScreenTestTags.MEMBERS_BUTTON),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_user_24),
            tint = TangemTheme.colors3.icon.secondary,
            contentDescription = null,
        )
        Text(
            modifier = Modifier.weight(1f),
            text = state.title.resolveReference(),
            color = TangemTheme.colors3.text.primary,
            style = TangemTheme.typography3.body.medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = state.membersCount.resolveReference(),
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.body.medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ManageTokensRow(state: AccountDetailsUM) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = state.onManageTokensClick)
            .padding(all = TangemTheme.dimens.spacing12)
            .testTag(AccountDetailsScreenTestTags.MANAGE_TOKENS_BUTTON),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_group_24),
            tint = TangemTheme.colors3.icon.secondary,
            contentDescription = null,
        )
        Text(
            text = stringResourceSafe(R.string.main_manage_tokens),
            color = TangemTheme.colors3.text.primary,
            style = TangemTheme.typography3.body.medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NetworksInfoText(state: AccountDetailsUM.NetworksInfoUM) {
    val text = state.text.resolveReference()
    val linkText = state.linkText.resolveReference()
    val linkColor = TangemTheme.colors3.text.brand
    Text(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .testTag(AccountDetailsScreenTestTags.NETWORKS_INFO),
        text = buildAnnotatedString {
            append(text)
            append(" ")
            withLink(
                LinkAnnotation.Clickable(
                    tag = NETWORKS_INFO_LINK_TAG,
                    styles = TextLinkStyles(style = SpanStyle(color = linkColor)),
                    linkInteractionListener = { state.onLinkClick() },
                ),
            ) {
                append(linkText)
            }
        },
        style = TangemTheme.typography3.caption.medium,
        color = TangemTheme.colors3.text.secondary,
    )
}

@Composable
private fun AccountRow(state: AccountDetailsUM) {
    val onEditClick = state.onAccountEditClick
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TangemTheme.dimens.radius12))
            .background(TangemTheme.colors3.bg.secondary)
            .then(if (onEditClick != null) Modifier.clickable(onClick = onEditClick) else Modifier)
            .padding(all = TangemTheme.dimens.spacing12)
            .testTag(AccountDetailsScreenTestTags.EDIT_ACCOUNT_BUTTON),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        AccountRowLegacy(
            title = state.accountName,
            subtitle = resourceReference(R.string.account_form_name),
            icon = state.accountIcon,
            modifier = Modifier.weight(1f),
            isReverse = true,
        )

        if (onEditClick != null) {
            SecondarySmallButton(
                config = SmallButtonConfig(
                    text = resourceReference(R.string.common_edit),
                    onClick = onEditClick,
                ),
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

private val archiveModeAvailable
    get() = AccountDetailsUM.ArchiveMode.Available(
        onArchiveAccountClick = {},
        isLoading = false,
    )

private class PreviewStateProvider : CollectionPreviewParameterProvider<AccountDetailsUM>(
    buildList {
        val accountName = "Main"
        val portfolioIcon = AccountIconPreviewData.randomAccountIcon()
        val first = AccountDetailsUM(
            onCloseClick = {},
            onAccountEditClick = {},
            onManageTokensClick = {},
            archiveMode = archiveModeAvailable,
            accountName = stringReference(accountName),
            accountIcon = portfolioIcon,
            isManageTokensAvailable = true,
        )
        add(first)
        add(first.copy(archiveMode = archiveModeAvailable.copy(isLoading = true)))
        add(first.copy(archiveMode = AccountDetailsUM.ArchiveMode.None))
        add(first.copy(isManageTokensAvailable = false))
        add(
            first.copy(
                onAccountEditClick = null,
                members = AccountDetailsUM.MembersRowUM(
                    title = stringReference("Members"),
                    membersCount = stringReference("5 people"),
                    onClick = {},
                ),
                networksInfo = AccountDetailsUM.NetworksInfoUM(
                    text = stringReference("Text about networks."),
                    linkText = stringReference("More info"),
                    onLinkClick = {},
                ),
            ),
        )
    },
)