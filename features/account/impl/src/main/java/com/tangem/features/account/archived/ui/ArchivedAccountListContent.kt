package com.tangem.features.account.archived.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.components.buttons.SecondarySmallButton
import com.tangem.core.ui.components.buttons.SmallButtonConfig
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.account.archived.entity.AccountArchivedUM
import com.tangem.features.account.archived.entity.ArchivedAccountUM
import com.tangem.features.account.common.toUM
import com.tangem.features.account.details.ui.AccountIcon
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun ArchivedAccountListContent(state: AccountArchivedUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(color = TangemTheme.colors.background.secondary)
            .fillMaxSize()
            .imePadding()
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppBarWithBackButton(
            text = stringResourceSafe(R.string.account_archived_title),
            onBackClick = state.onCloseClick,
            modifier = Modifier.height(TangemTheme.dimens.size56),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),

        ) {
            when (state) {
                is AccountArchivedUM.Content -> ArchiveAccountContent(state)
                is AccountArchivedUM.Error -> ArchiveAccountError(state)
                is AccountArchivedUM.Loading -> ArchiveAccountLoading()
            }
        }
    }
}

@Composable
private fun ArchiveAccountLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = TangemTheme.colors.icon.primary1,
            modifier = Modifier,
        )
    }
}

@Composable
private fun ArchiveAccountError(state: AccountArchivedUM.Error, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.caption2,
                text = stringResourceSafe(R.string.common_unable_to_load),
            )
            SecondarySmallButton(
                config = SmallButtonConfig(
                    text = resourceReference(R.string.try_to_load_data_again_button_title),
                    onClick = state.onRetryClick,
                ),
            )
        }
    }
}

@Composable
private fun ArchiveAccountContent(state: AccountArchivedUM.Content, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier) {
        itemsIndexed(
            items = state.accounts,
            key = { index, item -> item.accountId },
        ) { index, account ->
            ArchivedAccountRow(
                item = account,
                modifier = Modifier.roundedShapeItemDecoration(
                    backgroundColor = TangemTheme.colors.background.primary,
                    radius = TangemTheme.dimens.radius20,
                    currentIndex = index,
                    addDefaultPadding = true,
                    lastIndex = state.accounts.lastIndex,
                ),
            )
        }
    }
}

@Composable
private fun ArchivedAccountRow(item: ArchivedAccountUM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = { item.onClick(item.accountId) })
            .padding(all = TangemTheme.dimens.spacing12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        AccountIcon(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(9.dp)),
            accountName = item.accountName,
            accountIcon = item.accountIcon,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing2),
        ) {
            Text(
                text = item.accountName,
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.primary1,
            )
            Text(
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.caption2,
                text = item.tokensInfo.resolveReference(),
            )
        }

        SecondarySmallButton(
            config = SmallButtonConfig(
                text = resourceReference(R.string.account_archived_recover),
                onClick = { item.onClick(item.accountId) },
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WcConnectionsContentPreview(@PreviewParameter(PreviewStateProvider::class) params: AccountArchivedUM) {
    TangemThemePreview {
        ArchivedAccountListContent(state = params)
    }
}

@Suppress("MagicNumber")
private class PreviewStateProvider : CollectionPreviewParameterProvider<AccountArchivedUM>(
    buildList {
        fun portfolioIcon() = CryptoPortfolioIcon.ofDefaultCustomAccount().toUM()

        val firstList = List(10) {
            ArchivedAccountUM(
                accountId = it.toString(),
                accountName = "Account name",
                accountIcon = portfolioIcon(),
                tokensInfo = stringReference("10 tokens in 2 networks"),
                onClick = {},

            )
        }.toImmutableList()
        val first = AccountArchivedUM.Content(
            onCloseClick = {},
            accounts = firstList,
        )
        add(first)
        add(AccountArchivedUM.Loading {})
        add(AccountArchivedUM.Error({}, {}))
    },
)