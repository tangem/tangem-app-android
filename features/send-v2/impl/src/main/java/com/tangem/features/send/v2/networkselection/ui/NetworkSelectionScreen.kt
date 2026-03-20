package com.tangem.features.send.v2.networkselection.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.fields.SearchBar
import com.tangem.core.ui.components.token.TokenItem
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.extensions.pluralStringResourceSafe
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.v2.networkselection.entity.AccountGroupUM
import com.tangem.features.send.v2.networkselection.entity.NetworkSelectionUM
import com.tangem.features.send.v2.networkselection.entity.WalletGroupUM

private const val CHEVRON_EXPANDED_ROTATION = 180f
private const val CHEVRON_COLLAPSED_ROTATION = 0f

@Composable
internal fun NetworkSelectionScreen(state: NetworkSelectionUM, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TangemTheme.colors.background.tertiary)
                .systemBarsPadding()
                .imePadding(),
        ) {
            TangemTopAppBar(
                title = stringResourceSafe(R.string.common_token_send),
                startButton = TopAppBarButtonUM.Icon(
                    iconRes = R.drawable.ic_close_24,
                    onClicked = onDismiss,
                ),
            )
            SearchBar(
                state = state.searchBar,
                modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing22),
            )
            Spacer(modifier = Modifier.height(TangemTheme.dimens.spacing8))
            if (state.walletGroups.isEmpty() && state.searchBar.query.isNotBlank()) {
                NetworkSelectionEmpty(modifier = Modifier.weight(1f))
            } else {
                NetworkSelectionContent(state = state)
            }
        }
    }
}

@Composable
private fun NetworkSelectionEmpty(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = TangemTheme.dimens.spacing16),
    ) {
        Text(
            text = stringResourceSafe(R.string.common_no_results),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.tertiary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun NetworkSelectionContent(state: NetworkSelectionUM) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = TangemTheme.dimens.spacing12),
    ) {
        state.walletGroups.forEach { walletGroup ->
            item(key = "wallet_header_${walletGroup.userWalletId}") {
                WalletHeader(walletGroup = walletGroup)
            }
            walletGroup.accounts.forEach { accountGroup ->
                accountGroupItems(
                    walletGroup = walletGroup,
                    accountGroup = accountGroup,
                    isBalanceHidden = state.isBalanceHidden,
                )
            }
        }
    }
}

private fun LazyListScope.accountGroupItems(
    walletGroup: WalletGroupUM,
    accountGroup: AccountGroupUM,
    isBalanceHidden: Boolean,
) {
    val hasHiddenTokens = accountGroup.hiddenTokensCount > 0
    val lastIndex = accountGroup.tokens.lastIndex.inc() + if (hasHiddenTokens) 1 else 0
    item(key = "account_${walletGroup.userWalletId}_${accountGroup.accountName}") {
        AnimatedVisibility(
            visible = walletGroup.isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            AccountHeader(
                accountGroup = accountGroup,
                modifier = Modifier.roundedShapeItemDecoration(
                    currentIndex = 0,
                    lastIndex = lastIndex,
                    radius = TangemTheme.dimens.radius14,
                    backgroundColor = TangemTheme.colors.background.action,
                ),
            )
        }
    }
    itemsIndexed(
        items = accountGroup.tokens,
        key = { _, token -> "token_${walletGroup.userWalletId}_${token.id}" },
    ) { index, tokenState ->
        val indexWithHeader = index.inc()
        AnimatedVisibility(
            visible = walletGroup.isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            TokenItem(
                state = tokenState,
                isBalanceHidden = isBalanceHidden,
                reorderableTokenListState = null,
                modifier = Modifier.roundedShapeItemDecoration(
                    currentIndex = indexWithHeader,
                    lastIndex = lastIndex,
                    radius = TangemTheme.dimens.radius14,
                    backgroundColor = TangemTheme.colors.background.action,
                ),
            )
        }
    }
    if (hasHiddenTokens) {
        item(
            key = "hidden_${walletGroup.userWalletId}_${accountGroup.accountName}",
        ) {
            AnimatedVisibility(
                visible = walletGroup.isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                HiddenTokensFooter(
                    count = accountGroup.hiddenTokensCount,
                    modifier = Modifier.roundedShapeItemDecoration(
                        currentIndex = lastIndex,
                        lastIndex = lastIndex,
                        radius = TangemTheme.dimens.radius14,
                        backgroundColor = TangemTheme.colors.background.action,
                    ),
                )
            }
        }
    }
}

@Composable
private fun WalletHeader(walletGroup: WalletGroupUM) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (walletGroup.isExpanded) CHEVRON_COLLAPSED_ROTATION else CHEVRON_EXPANDED_ROTATION,
        label = "chevron_rotation",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = walletGroup.onExpandToggle)
            .padding(
                horizontal = TangemTheme.dimens.spacing16,
                vertical = TangemTheme.dimens.spacing12,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = walletGroup.walletName,
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.primary1,
            modifier = Modifier.weight(1f),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(TangemTheme.dimens.size24)
                .background(
                    color = TangemTheme.colors.button.secondary,
                    shape = CircleShape,
                ),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_chevron_up_24),
                contentDescription = null,
                tint = TangemTheme.colors.icon.informative,
                modifier = Modifier
                    .size(TangemTheme.dimens.size16)
                    .rotate(chevronRotation),
            )
        }
    }
}

@Composable
private fun AccountHeader(accountGroup: AccountGroupUM, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = TangemTheme.dimens.spacing12,
                top = TangemTheme.dimens.spacing10,
                bottom = TangemTheme.dimens.spacing6,
                end = TangemTheme.dimens.spacing12,
            ),
    ) {
        accountGroup.iconState?.let { iconState ->
            CurrencyIcon(
                state = iconState,
                modifier = Modifier.size(TangemTheme.dimens.size18),
            )
            Spacer(modifier = Modifier.width(TangemTheme.dimens.spacing6))
        }
        Text(
            text = accountGroup.accountName.resolveReference(),
            style = TangemTheme.typography.caption1,
            color = TangemTheme.colors.text.tertiary,
        )
    }
}

@Composable
private fun HiddenTokensFooter(count: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        HorizontalDivider(
            thickness = 0.5.dp,
            color = TangemTheme.colors.stroke.primary,
            modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing14),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = TangemTheme.dimens.spacing12,
                    end = TangemTheme.dimens.spacing12,
                    top = TangemTheme.dimens.spacing10,
                    bottom = TangemTheme.dimens.spacing10,
                ),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_eye_off_outline_24),
                contentDescription = null,
                tint = TangemTheme.colors.icon.informative,
                modifier = Modifier.size(TangemTheme.dimens.size16),
            )
            Spacer(modifier = Modifier.width(TangemTheme.dimens.spacing8))
            Text(
                text = pluralStringResourceSafe(
                    id = R.plurals.send_network_selection_hidden_tokens,
                    count = count,
                    count,
                ),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
            )
        }
    }
}