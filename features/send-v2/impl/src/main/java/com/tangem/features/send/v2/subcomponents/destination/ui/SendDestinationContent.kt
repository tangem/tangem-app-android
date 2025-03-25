package com.tangem.features.send.v2.subcomponents.destination.ui

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
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.containers.FooterContainer
import com.tangem.core.ui.components.inputrow.InputRowRecipient
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.v2.impl.R
import com.tangem.features.send.v2.subcomponents.destination.analytics.EnterAddressSource
import com.tangem.features.send.v2.subcomponents.destination.model.SendDestinationClickIntents
import com.tangem.features.send.v2.subcomponents.destination.ui.state.DestinationRecipientListUM
import com.tangem.features.send.v2.subcomponents.destination.ui.state.DestinationTextFieldUM
import com.tangem.features.send.v2.subcomponents.destination.ui.state.DestinationUM
import kotlinx.collections.immutable.ImmutableList

private const val ADDRESS_FIELD_KEY = "ADDRESS_FIELD_KEY"
private const val MEMO_FIELD_KEY = "MEMO_FIELD_KEY"

@Composable
internal fun SendDestinationContent(
    state: DestinationUM,
    clickIntents: SendDestinationClickIntents,
    isBalanceHidden: Boolean,
) {
    if (state !is DestinationUM.Content) return
    val recipients = state.recent
    val wallets = state.wallets
    val memoField = state.memoTextField
    val address = state.addressTextField
    val isValidating by remember(state.isValidating) { derivedStateOf { state.isValidating } }
    val isError by remember(address.isError) { derivedStateOf { address.isError } }
    LazyColumn(
        modifier = Modifier // Do not put fillMaxSize() in here
            .background(TangemTheme.colors.background.tertiary)
            .padding(
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp,
            ),
    ) {
        addressItem(
            address = address,
            networkName = state.networkName,
            isError = isError,
            isValidating = isValidating,
            onAddressChange = clickIntents::onRecipientAddressValueChange,
        )
        memoField(
            memoField = memoField,
            onMemoChange = clickIntents::onRecipientMemoValueChange,
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
    address: DestinationTextFieldUM.RecipientAddress,
    networkName: String,
    isError: Boolean,
    isValidating: Boolean,
    onAddressChange: (String, EnterAddressSource) -> Unit,
) {
    item(key = ADDRESS_FIELD_KEY) {
        FooterContainer(
            footer = resourceReference(R.string.send_recipient_address_footer, wrappedList(networkName)),
        ) {
            InputRowRecipient(
                value = address.value,
                title = address.label,
                placeholder = address.placeholder,
                onValueChange = { onAddressChange(it, EnterAddressSource.InputField) },
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

private fun LazyListScope.memoField(
    memoField: DestinationTextFieldUM.RecipientMemo?,
    onMemoChange: (String, Boolean) -> Unit,
) {
    if (memoField != null) {
        item(key = MEMO_FIELD_KEY) {
            val placeholder = if (memoField.isEnabled) memoField.placeholder else memoField.disabledText
            TextFieldWithPaste(
                value = memoField.value,
                label = memoField.label,
                placeholder = placeholder,
                footer = resourceReference(R.string.send_recipient_memo_footer),
                onValueChange = { onMemoChange(it, false) },
                onPasteClick = { onMemoChange(it, true) },
                modifier = Modifier.padding(top = 20.dp),
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
                20.dp to 12.dp
            } else {
                0.dp to 8.dp
            }
            val topRadius = if (isFirst) 16.dp else 0.dp
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
                        bottom = 12.dp,
                        start = 12.dp,
                        end = 12.dp,
                    ),
            )
        }
    }
}

private fun LazyListScope.listItem(
    list: ImmutableList<DestinationRecipientListUM>,
    clickIntents: SendDestinationClickIntents,
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

        if (item.isVisible) {
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
                    .animateItem()
                    .then(
                        if (isLast && index == list.lastIndex) {
                            Modifier
                                .clip(
                                    shape = RoundedCornerShape(
                                        bottomStart = 16.dp,
                                        bottomEnd = 16.dp,
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