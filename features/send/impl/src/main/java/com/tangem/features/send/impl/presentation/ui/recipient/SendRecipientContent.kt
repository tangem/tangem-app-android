package com.tangem.features.send.impl.presentation.ui.recipient

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.tangem.core.ui.components.inputrow.InputRowRecipient
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.domain.SendRecipientListContent
import com.tangem.features.send.impl.presentation.state.SendStates
import com.tangem.features.send.impl.presentation.ui.common.FooterContainer
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents

private const val ADDRESS_FIELD_KEY = "ADDRESS_FIELD_KEY"
private const val MEMO_FIELD_KEY = "MEMO_FIELD_KEY"
private const val MY_WALLETS_HEADER_KEY = "MY_WALLETS_HEADER_KEY"

@Composable
internal fun SendRecipientContent(
    uiState: SendStates.RecipientState?,
    clickIntents: SendClickIntents,
    recipientList: LazyPagingItems<SendRecipientListContent>,
) {
    if (uiState == null) return
    val address = uiState.addressTextField
    val isValidating by remember(uiState.isValidating) { derivedStateOf { uiState.isValidating } }
    val isError by remember(address.isError) { derivedStateOf { address.isError } }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TangemTheme.colors.background.tertiary)
            .padding(horizontal = TangemTheme.dimens.spacing16),
    ) {
        item(key = ADDRESS_FIELD_KEY) {
            FooterContainer(
                footer = stringResource(R.string.send_recipient_address_footer, uiState.network),
            ) {
                InputRowRecipient(
                    value = address.value,
                    title = address.label,
                    placeholder = address.placeholder,
                    onValueChange = address.onValueChange,
                    onPasteClick = clickIntents::onRecipientAddressValueChange,
                    singleLine = true,
                    isError = isError,
                    isLoading = isValidating,
                    error = address.error,
                    modifier = Modifier
                        .padding(top = TangemTheme.dimens.spacing4)
                        .background(
                            color = TangemTheme.colors.background.action,
                            shape = TangemTheme.shapes.roundedCornersXMedium,
                        ),
                )
            }
        }
        uiState.memoTextField?.let { memoField ->
            item(key = MEMO_FIELD_KEY) {
                TextFieldWithPaste(
                    value = memoField.value,
                    label = memoField.label,
                    placeholder = memoField.placeholder,
                    footer = stringResource(R.string.send_recipient_memo_footer),
                    onValueChange = memoField.onValueChange,
                    onPasteClick = clickIntents::onRecipientMemoValueChange,
                    modifier = Modifier.padding(top = TangemTheme.dimens.spacing20),
                    isError = memoField.isError,
                    error = memoField.error,
                )
            }
        }
        recipientListItem(
            recipientList = recipientList,
            clickIntents = clickIntents,
        )
    }
}

@Suppress("LongMethod")
@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.recipientListItem(
    recipientList: LazyPagingItems<SendRecipientListContent>,
    clickIntents: SendClickIntents,
) {
    items(
        count = recipientList.itemCount,
        key = recipientList.itemKey {
            when (it) {
                is SendRecipientListContent.Wallets -> MY_WALLETS_HEADER_KEY
                is SendRecipientListContent.Item -> it.id
            }
        },
        contentType = recipientList.itemContentType { it::class.java },
    ) { index ->
        recipientList[index]?.let { item ->
            when (item) {
                is SendRecipientListContent.Wallets -> {
                    RecipientWalletListItem(
                        item = item,
                        clickIntents = clickIntents,
                        modifier = Modifier
                            .animateItemPlacement()
                            .padding(top = TangemTheme.dimens.spacing20)
                            .then(
                                if (index == 0) {
                                    val bottomRadius = if (item.isWalletsOnly) {
                                        TangemTheme.dimens.radius12
                                    } else {
                                        TangemTheme.dimens.radius0
                                    }
                                    Modifier.clip(
                                        RoundedCornerShape(
                                            topEnd = TangemTheme.dimens.radius12,
                                            topStart = TangemTheme.dimens.radius12,
                                            bottomStart = bottomRadius,
                                            bottomEnd = bottomRadius,
                                        ),
                                    )
                                } else {
                                    Modifier
                                },
                            ),
                    )
                }
                is SendRecipientListContent.Item -> {
                    val title = item.title.resolveReference()
                    ListItemWithIcon(
                        title = item.title.resolveReference(),
                        subtitle = item.subtitle.resolveReference(),
                        info = item.timestamp?.resolveReference(),
                        subtitleEndOffset = item.subtitleEndOffset,
                        subtitleIconRes = item.subtitleIconRes,
                        modifier = Modifier
                            .then(
                                if (index == recipientList.itemCount - 1) {
                                    Modifier
                                        .padding(bottom = TangemTheme.dimens.spacing20)
                                        .clip(
                                            RoundedCornerShape(
                                                bottomEnd = TangemTheme.dimens.radius12,
                                                bottomStart = TangemTheme.dimens.radius12,
                                            ),
                                        )
                                } else {
                                    Modifier
                                },
                            )
                            .background(TangemTheme.colors.background.action),
                        onClick = { clickIntents.onRecipientAddressValueChange(title) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RecipientWalletListItem(
    item: SendRecipientListContent.Wallets,
    clickIntents: SendClickIntents,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(TangemTheme.colors.background.action)
            .padding(top = TangemTheme.dimens.spacing12),
    ) {
        if (item.list.isNotEmpty()) {
            Text(
                text = stringResource(R.string.send_recipient_wallets_title),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = TangemTheme.dimens.spacing12,
                        end = TangemTheme.dimens.spacing12,
                        bottom = TangemTheme.dimens.spacing8,
                    ),
            )
        }
        item.list.forEachIndexed { _, wallet ->
            val title = wallet.title.resolveReference()
            ListItemWithIcon(
                title = wallet.title.resolveReference(),
                subtitle = wallet.subtitle.resolveReference(),
                onClick = { clickIntents.onRecipientAddressValueChange(title) },
            )
        }
        if (!item.isWalletsOnly) {
            val topPadding = if (item.list.isNotEmpty()) {
                TangemTheme.dimens.spacing8
            } else {
                TangemTheme.dimens.spacing0
            }
            Text(
                text = stringResource(R.string.send_recent_transactions),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = topPadding,
                        bottom = TangemTheme.dimens.spacing8,
                        start = TangemTheme.dimens.spacing12,
                        end = TangemTheme.dimens.spacing12,
                    ),
            )
        }
    }
}