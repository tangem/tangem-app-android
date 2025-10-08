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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.common.ui.account.AccountIconPreviewData
import com.tangem.common.ui.userwallet.UserWalletItem
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.common.ui.userwallet.state.UserWalletItemUM.ImageState
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.account.impl.R
import com.tangem.features.account.selector.entity.PortfolioSelectorItemUM
import com.tangem.features.account.selector.entity.PortfolioSelectorUM
import com.tangem.features.account.selector.ui.PortfolioSelectorPreviewData.firstList
import com.tangem.features.account.selector.ui.PortfolioSelectorPreviewData.lockedWalletList
import com.tangem.features.account.selector.ui.PortfolioSelectorPreviewData.secondList
import com.tangem.features.account.selector.ui.PortfolioSelectorPreviewData.walletList
import kotlinx.collections.immutable.toImmutableList
import java.util.UUID

private const val DISABLED_WALLET_ALPHA = 0.5f

@Composable
internal fun PortfolioSelectorContent(
    state: PortfolioSelectorUM,
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
                item is PortfolioSelectorItemUM.GroupTitle -> Modifier.padding(
                    top = TangemTheme.dimens.spacing16,
                )
                else -> Modifier.padding(
                    top = TangemTheme.dimens.spacing8,
                )
            }

            when (item) {
                is PortfolioSelectorItemUM.Portfolio -> UserWalletItem(
                    state = item.item,
                    modifier = offsetModifier
                        .clip(RoundedCornerShape(TangemTheme.dimens.radius14))
                        .background(color = TangemTheme.colors.background.primary)
                        .let { if (!item.item.isEnabled) it.alpha(DISABLED_WALLET_ALPHA) else it },
                )
                is PortfolioSelectorItemUM.GroupTitle -> WalletNameRow(
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
private fun WalletNameRow(model: PortfolioSelectorItemUM.GroupTitle, modifier: Modifier = Modifier) {
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
private fun Preview(@PreviewParameter(PortfolioSelectorPreviewStateProvider::class) params: PortfolioSelectorUM) {
    TangemThemePreview {
        PortfolioSelectorContent(
            state = params,
            modifier = Modifier.background(color = TangemTheme.colors.background.secondary),
        )
    }
}

internal object PortfolioSelectorPreviewData {

    val accountName get() = stringReference(value = "Portfolio")
    val walletName get() = stringReference(value = "Tangem 2.0")

    private val accountItem: UserWalletItemUM
        get() = UserWalletItemUM(
            id = UserWalletId(UUID.randomUUID().toString().encodeToByteArray()),
            name = accountName,
            information = UserWalletItemUM.Information.Loaded(stringReference("12 tokens")),
            balance = UserWalletItemUM.Balance.Loaded("$726.04", false),
            isEnabled = true,
            onClick = { },
            imageState = ImageState.Account(
                name = accountName,
                icon = AccountIconPreviewData.randomAccountIcon(),
            ),
            label = null,
        )

    private val lockedAccountItem: UserWalletItemUM
        get() = accountItem.copy(isEnabled = false)

    private val walletItem: UserWalletItemUM
        get() = UserWalletItemUM(
            id = UserWalletId(UUID.randomUUID().toString().encodeToByteArray()),
            name = walletName,
            information = UserWalletItemUM.Information.Loaded(stringReference("12 tokens")),
            balance = UserWalletItemUM.Balance.Loaded("$726.04", false),
            isEnabled = true,
            onClick = { },
            imageState = ImageState.MobileWallet,
            label = null,
        )

    private val lockedWalletItem: UserWalletItemUM
        get() = walletItem.copy(isEnabled = false)

    val firstList
        get() = buildList {
            PortfolioSelectorItemUM.GroupTitle(
                id = UUID.randomUUID().toString(),
                name = stringReference("Tangem 2.0"),
            ).let(::add)
            accountItem
                .let { PortfolioSelectorItemUM.Portfolio(it) }
                .let(::add)
            lockedAccountItem
                .let { PortfolioSelectorItemUM.Portfolio(it) }
                .let(::add)
            PortfolioSelectorItemUM.GroupTitle(
                id = UUID.randomUUID().toString(),
                name = stringReference("Tangem White"),
            ).let(::add)
            accountItem.let { PortfolioSelectorItemUM.Portfolio(it) }
                .let(::add)
        }

    val secondList
        get() = firstList + buildList {
            PortfolioSelectorItemUM.GroupTitle(
                id = UUID.randomUUID().toString(),
                name = resourceReference(R.string.common_locked_wallets),
            ).let(::add)
            add(PortfolioSelectorItemUM.Portfolio(lockedWalletItem))
            add(PortfolioSelectorItemUM.Portfolio(lockedWalletItem))
        }

    val walletList
        get() = buildList {
            add(PortfolioSelectorItemUM.Portfolio(walletItem))
            add(PortfolioSelectorItemUM.Portfolio(walletItem))
        }

    val lockedWalletList
        get() = buildList {
            add(PortfolioSelectorItemUM.Portfolio(walletItem))
            val title = PortfolioSelectorItemUM.GroupTitle(
                id = UUID.randomUUID().toString(),
                name = resourceReference(R.string.common_locked_wallets),
            )
            add(title)
            add(PortfolioSelectorItemUM.Portfolio(lockedWalletItem))
            add(PortfolioSelectorItemUM.Portfolio(lockedWalletItem))
        }
}

internal class PortfolioSelectorPreviewStateProvider : CollectionPreviewParameterProvider<PortfolioSelectorUM>(
    buildList {
        val first = PortfolioSelectorUM(
            title = resourceReference(R.string.common_choose_account),
            items = firstList.toImmutableList(),
        )
        val second = PortfolioSelectorUM(
            title = resourceReference(R.string.common_choose_account),
            items = secondList.toImmutableList(),
        )
        val walletListUM = PortfolioSelectorUM(
            title = resourceReference(R.string.common_choose_wallet),
            items = walletList.toImmutableList(),
        )
        val lockedWalletListUM =
            PortfolioSelectorUM(
                title = resourceReference(R.string.common_choose_wallet),
                items = lockedWalletList.toImmutableList(),
            )
        add(first)
        add(second)
        add(walletListUM)
        add(lockedWalletListUM)
    },
)