package com.tangem.features.send.impl.presentation.ui.recipient

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.components.containers.FooterContainer
import com.tangem.core.ui.components.inputrow.InputRowRecipient
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.analytics.EnterAddressSource
import com.tangem.features.send.impl.presentation.domain.SendRecipientListContent
import com.tangem.features.send.impl.presentation.state.SendStates
import com.tangem.features.send.impl.presentation.state.fields.SendTextField
import com.tangem.features.send.impl.presentation.state.previewdata.RecipientStatePreviewData
import com.tangem.features.send.impl.presentation.state.previewdata.SendClickIntentsStub
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import kotlinx.collections.immutable.ImmutableList

private const val ADDRESS_FIELD_KEY = "ADDRESS_FIELD_KEY"
private const val MEMO_FIELD_KEY = "MEMO_FIELD_KEY"

@Composable
internal fun SendRecipientContent(
    uiState: SendStates.RecipientState?,
    clickIntents: SendClickIntents,
    isBalanceHidden: Boolean,
) {
    if (uiState == null) return
    val recipients = uiState.recent
    val wallets = uiState.wallets
    val memoField = uiState.memoTextField
    val address = uiState.addressTextField
    val isValidating by remember(uiState.isValidating) { derivedStateOf { uiState.isValidating } }
    val isError by remember(address.isError) { derivedStateOf { address.isError } }
    LazyColumn(
        modifier = Modifier // Do not put fillMaxSize() in here
            .background(TangemTheme.colors.background.tertiary)
            .padding(
                start = TangemTheme.dimens.spacing16,
                end = TangemTheme.dimens.spacing16,
                bottom = TangemTheme.dimens.spacing16,
            ),
    ) {
        addressItem(
            address = address,
            network = uiState.network,
            isError = isError,
            isValidating = isValidating,
            onAddressChange = clickIntents::onRecipientAddressValueChange,
        )
        memoField(
            memoField = memoField,
            onMemoChange = { clickIntents.onRecipientMemoValueChange(it, true) },
        )
        listHeaderItem(
            titleRes = R.string.send_recipient_wallets_title,
            isVisible = wallets.isNotEmpty() && wallets.first().isVisible,
            isFirst = true,
        )
        listItem(
            list = wallets,
            clickIntents = clickIntents,
            isLast = recipients.any { !it.isVisible },
            isBalanceHidden = isBalanceHidden,
        )
        listHeaderItem(
            titleRes = R.string.send_recent_transactions,
            isVisible = recipients.isNotEmpty() && recipients.first().isVisible,
            isFirst = wallets.any { !it.isVisible },
        )
        listItem(
            list = recipients,
            clickIntents = clickIntents,
            isLast = true,
            isBalanceHidden = isBalanceHidden,
        )
    }
}

private fun LazyListScope.addressItem(
    address: SendTextField.RecipientAddress,
    network: String,
    isError: Boolean,
    isValidating: Boolean,
    onAddressChange: (String, EnterAddressSource?) -> Unit,
) {
    item(key = ADDRESS_FIELD_KEY) {
        FooterContainer(
            footer = resourceReference(R.string.send_recipient_address_footer, wrappedList(network)),
        ) {
            InputRowRecipient(
                value = address.value,
                title = address.label,
                placeholder = address.placeholder,
                onValueChange = address.onValueChange,
                onPasteClick = { onAddressChange(it, EnterAddressSource.PasteButton) },
                isError = isError,
                isLoading = isValidating,
                error = address.error,
                isValuePasted = address.isValuePasted,
                modifier = Modifier
                    .background(
                        color = TangemTheme.colors.background.action,
                        shape = TangemTheme.shapes.roundedCornersXMedium,
                    ),
            )
        }
    }
}

private fun LazyListScope.memoField(memoField: SendTextField.RecipientMemo?, onMemoChange: (String) -> Unit) {
    if (memoField != null) {
        item(key = MEMO_FIELD_KEY) {
            val placeholder = if (memoField.isEnabled) memoField.placeholder else memoField.disabledText
            TextFieldWithPaste(
                value = memoField.value,
                label = memoField.label,
                placeholder = placeholder,
                footer = resourceReference(R.string.send_recipient_memo_footer),
                onValueChange = memoField.onValueChange,
                onPasteClick = onMemoChange,
                modifier = Modifier.padding(top = TangemTheme.dimens.spacing20),
                labelStyle = TangemTheme.typography.subtitle2,
                isError = memoField.isError,
                error = memoField.error,
                isReadOnly = !memoField.isEnabled,
                isValuePasted = memoField.isValuePasted,
            )
        }
    }
}

private fun LazyListScope.listHeaderItem(@StringRes titleRes: Int, isVisible: Boolean, isFirst: Boolean) {
    item(key = titleRes) {
        AnimateRecentAppearance(isVisible) {
            val (topPadding, paddingFromTop) = if (isFirst) {
                TangemTheme.dimens.spacing20 to TangemTheme.dimens.spacing12
            } else {
                TangemTheme.dimens.spacing0 to TangemTheme.dimens.spacing8
            }
            val topRadius = if (isFirst) {
                TangemTheme.dimens.radius16
            } else {
                TangemTheme.dimens.radius0
            }
            Text(
                text = stringResourceSafe(titleRes),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = topPadding)
                    .clip(
                        RoundedCornerShape(
                            topEnd = topRadius,
                            topStart = topRadius,
                        ),
                    )
                    .background(TangemTheme.colors.background.action)
                    .padding(
                        top = paddingFromTop,
                        bottom = TangemTheme.dimens.spacing12,
                        start = TangemTheme.dimens.spacing12,
                        end = TangemTheme.dimens.spacing12,
                    ),
            )
        }
    }
}

private fun LazyListScope.listItem(
    list: ImmutableList<SendRecipientListContent>,
    clickIntents: SendClickIntents,
    isLast: Boolean,
    isBalanceHidden: Boolean,
) {
    items(
        count = list.size,
        key = { list[it].id },
        contentType = { list[it]::class.java },
    ) { index ->
        val item = list[index]
        val title = item.title.resolveReference()
        AnimateRecentAppearance(item.isVisible) {
            ListItemWithIcon(
                title = title,
                subtitle = item.subtitle.orMaskWithStars(isBalanceHidden).resolveReference(),
                info = item.timestamp?.resolveReference(),
                subtitleEndOffset = item.subtitleEndOffset,
                subtitleIconRes = item.subtitleIconRes,
                onClick = {
                    clickIntents.onRecipientAddressValueChange(
                        title,
                        EnterAddressSource.RecentAddress,
                    )
                },
                isLoading = item.isLoading,
                modifier = Modifier
                    .then(
                        if (isLast && index == list.lastIndex) {
                            Modifier
                                .clip(
                                    shape = RoundedCornerShape(
                                        bottomStart = TangemTheme.dimens.radius16,
                                        bottomEnd = TangemTheme.dimens.radius16,
                                    ),
                                )
                        } else {
                            Modifier
                        },
                    )
                    .background(TangemTheme.colors.background.action),
            )
        }
    }
}

@Composable
private fun AnimateRecentAppearance(isVisible: Boolean, content: @Composable () -> Unit) {
    AnimatedContent(
        targetState = isVisible,
        label = "Item Appearance Animation",
        transitionSpec = {
            (slideInHorizontally() + fadeIn())
                .togetherWith(slideOutVertically() + fadeOut())
        },
    ) {
        if (it) {
            content()
        } else {
            Box(modifier = Modifier.fillMaxWidth())
        }
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SendRecipientContent_Preview(
    @PreviewParameter(SendRecipientContentPreviewProvider::class) recipientState: SendStates.RecipientState,
) {
    TangemThemePreview {
        SendRecipientContent(
            uiState = recipientState,
            clickIntents = SendClickIntentsStub,
            isBalanceHidden = false,
        )
    }
}

private class SendRecipientContentPreviewProvider : PreviewParameterProvider<SendStates.RecipientState> {
    override val values: Sequence<SendStates.RecipientState>
        get() = sequenceOf(
            RecipientStatePreviewData.recipientWithRecentState,
            RecipientStatePreviewData.recipientState,
            RecipientStatePreviewData.recipientAddressState,
        )
}
// endregion