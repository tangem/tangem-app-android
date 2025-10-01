package com.tangem.features.account.selector.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.token.AccountItemPreviewData
import com.tangem.core.ui.components.token.TokenItem
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.account.selector.entity.AccountSelectorItemUM
import com.tangem.features.account.selector.entity.AccountSelectorUM
import com.tangem.features.account.selector.ui.AccountSelectorPreviewData.firstList
import kotlinx.collections.immutable.toImmutableList
import java.util.UUID

@Composable
internal fun AccountSelectorContent(
    state: AccountSelectorUM,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
    ) {
        val items = state.items
        itemsIndexed(
            items = items,
            key = { _, item -> item.id },
        ) { index, item ->
            val previewItem = items.getOrNull(index.dec())
            val offsetModifier = when {
                previewItem == null -> Modifier
                state.isSingleWallet -> Modifier.padding(
                    top = TangemTheme.dimens.spacing16,
                )
                item is AccountSelectorItemUM.Wallet -> Modifier.padding(
                    top = TangemTheme.dimens.spacing16,
                )
                else -> Modifier.padding(
                    top = TangemTheme.dimens.spacing8,
                )
            }

            when (item) {
                is AccountSelectorItemUM.Account -> TokenItem(
                    state = item.account,
                    isBalanceHidden = item.isBalanceHidden,
                    modifier = offsetModifier
                        .clip(RoundedCornerShape(TangemTheme.dimens.radius14))
                        .background(color = TangemTheme.colors.background.primary),
                )
                is AccountSelectorItemUM.Wallet -> WalletNameRow(
                    model = item,
                    modifier = offsetModifier
                        .fillMaxWidth()
                        .padding(horizontal = TangemTheme.dimens.spacing16),
                )
            }
        }
    }
}

@Composable
private fun WalletNameRow(model: AccountSelectorItemUM.Wallet, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = model.name.resolveReference(),
        style = TangemTheme.typography.subtitle2,
        color = TangemTheme.colors.text.tertiary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AccountSelectorContentPreview(
    @PreviewParameter(AccountSelectorPreviewStateProvider::class) params: AccountSelectorUM,
) {
    TangemThemePreview {
        AccountSelectorContent(
            state = params,
            modifier = Modifier.background(color = TangemTheme.colors.background.secondary),
        )
    }
}

internal object AccountSelectorPreviewData {
    val firstList
        get() = buildList {
            AccountSelectorItemUM.Wallet(
                id = UUID.randomUUID().toString(),
                name = stringReference("Tangem 2.0"),
            ).let(::add)
            AccountItemPreviewData.accountItem
                .let { AccountSelectorItemUM.Account(it, false) }
                .let(::add)
            AccountItemPreviewData.accountItem.copy(iconState = AccountItemPreviewData.accountLetterIcon)
                .let { AccountSelectorItemUM.Account(it, false) }
                .let(::add)
            AccountSelectorItemUM.Wallet(
                id = UUID.randomUUID().toString(),
                name = stringReference("Tangem White"),
            ).let(::add)
            AccountItemPreviewData.accountItem.copy(iconState = AccountItemPreviewData.accountLetterIcon)
                .let { AccountSelectorItemUM.Account(it, false) }
                .let(::add)
        }
}

internal class AccountSelectorPreviewStateProvider : CollectionPreviewParameterProvider<AccountSelectorUM>(
    buildList {
        val secondList = listOf(
            AccountItemPreviewData.accountItem,
            AccountItemPreviewData.accountItem.copy(iconState = AccountItemPreviewData.accountLetterIcon),
        ).map { AccountSelectorItemUM.Account(it, false) }

        val first = AccountSelectorUM(
            items = firstList.toImmutableList(),
            isSingleWallet = false,
        )
        val second = AccountSelectorUM(
            items = secondList.toImmutableList(),
            isSingleWallet = true,
        )
        add(first)
        add(second)
    },
)