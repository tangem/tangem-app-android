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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.containers.FooterContainer
import com.tangem.core.ui.components.inputrow.InputRowRecipient
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.test.SendAddressScreenTestTags
import com.tangem.core.ui.utils.GlobalMultipleClickPreventer
import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationRecipientListUM
import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationTextFieldUM
import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.v2.impl.R
import com.tangem.features.send.v2.subcomponents.destination.analytics.EnterAddressSource
import com.tangem.features.send.v2.subcomponents.destination.model.SendDestinationClickIntents
import kotlinx.collections.immutable.ImmutableList

private const val ADDRESS_FIELD_KEY = "ADDRESS_FIELD_KEY"
private const val MEMO_FIELD_KEY = "MEMO_FIELD_KEY"

@Suppress("LongMethod")
@Composable
internal fun SendDestinationContent(
    state: DestinationUM,
    clickIntents: SendDestinationClickIntents,
    isBalanceHidden: Boolean,
) {
    if (state !is DestinationUM.Content) return
    val recipients = state.recent
    val wallets = state.wallets
    val address = state.addressTextField
    val isValidating by remember(state.isValidating) { derivedStateOf { state.isValidating } }
    val isError by remember(address.isError) { derivedStateOf { address.isError } }
    LazyColumn(
        modifier = Modifier // Do not put fillMaxSize() in here
            .background(TangemTheme.colors.background.tertiary)
            .padding(horizontal = 16.dp)
            .testTag(SendAddressScreenTestTags.CONTAINER),
    ) {
        addressItem(
            address = address,
            networkName = state.networkName,
            isError = isError,
            isValidating = isValidating,
            onAddressChange = clickIntents::onRecipientAddressValueChange,
            onQrCodeClick = clickIntents::onQrCodeScanClick,
        )
        memoField(
            memoField = state.memoTextField,
            onMemoChange = clickIntents::onRecipientMemoValueChange,
        )
        listHeaderItem(
            titleRes = if (state.isAccountsMode == true) {
                R.string.common_accounts
            } else {
                R.string.send_recipient_wallets_title
            },
            isLoading = state.isAccountsMode == null,
            isVisible = wallets.isNotEmpty() && wallets.first().isVisible && !state.isRecentHidden,
            isFirst = true,
        )
        listItem(
            list = wallets,
            isLast = recipients.any { !it.isVisible },
            isBalanceHidden = false,
            isRecentHidden = state.isRecentHidden,
            onClick = { title ->
                clickIntents.onRecipientAddressValueChange(
                    title,
                    EnterAddressSource.MyWallets,
                )
            },
        )
        listHeaderItem(
            titleRes = R.string.send_recent_transactions,
            isLoading = state.isAccountsMode == null,
            isVisible = recipients.isNotEmpty() && recipients.first().isVisible && !state.isRecentHidden,
            isFirst = wallets.any { !it.isVisible },
        )
        listItem(
            list = recipients,
            isLast = true,
            isBalanceHidden = isBalanceHidden,
            isRecentHidden = state.isRecentHidden,
            onClick = { title ->
                clickIntents.onRecipientAddressValueChange(
                    title,
                    EnterAddressSource.RecentAddress,
                )
            },
        )
        item("SPACER_KEY") {
            SpacerH(16.dp)
        }
    }
}

@Suppress("LongParameterList")
private fun LazyListScope.addressItem(
    address: DestinationTextFieldUM.RecipientAddress,
    networkName: String,
    isError: Boolean,
    isValidating: Boolean,
    onAddressChange: (String, EnterAddressSource) -> Unit,
    onQrCodeClick: () -> Unit,
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
                onPasteClick = {
                    GlobalMultipleClickPreventer.processEvent {
                        onAddressChange(it, EnterAddressSource.PasteButton)
                    }
                },
                onQrCodeClick = onQrCodeClick,
                isError = isError,
                isLoading = isValidating,
                error = address.error,
                isValuePasted = address.isValuePasted,
                modifier = Modifier
                    .background(
                        color = TangemTheme.colors.background.action,
                        shape = TangemTheme.shapes.roundedCornersXMedium,
                    ),
                resolvedAddress = address.blockchainAddress,
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
                footer = annotatedReference(
                    buildAnnotatedString {
                        append(stringResourceSafe(R.string.send_recipient_memo_footer_v2))
                        append("\n")
                        withStyle(SpanStyle(fontWeight = FontWeight.Medium)) {
                            appendColored(
                                text = stringResourceSafe(
                                    R.string.send_recipient_memo_footer_v2_highlighted,
                                ),
                                color = TangemTheme.colors.text.secondary,
                            )
                        }
                    },
                ),
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

private fun LazyListScope.listHeaderItem(
    @StringRes titleRes: Int,
    isVisible: Boolean,
    isLoading: Boolean,
    isFirst: Boolean,
) {
    item(key = titleRes) {
        AnimateRecentAppearance(isVisible) {
            val (topPadding, paddingFromTop) = if (isFirst) {
                20.dp to 12.dp
            } else {
                0.dp to 8.dp
            }
            val topRadius = if (isFirst) 16.dp else 0.dp
            AnimatedContent(
                targetState = isLoading,
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
            ) { currentIsLoading ->
                if (currentIsLoading) {
                    Box {
                        TextShimmer(
                            style = TangemTheme.typography.subtitle2,
                            text = stringResourceSafe(titleRes),
                        )
                    }
                } else {
                    Text(
                        text = stringResourceSafe(titleRes),
                        style = TangemTheme.typography.subtitle2,
                        color = TangemTheme.colors.text.tertiary,
                    )
                }
            }
        }
    }
}

private fun LazyListScope.listItem(
    list: ImmutableList<DestinationRecipientListUM>,
    isLast: Boolean,
    isBalanceHidden: Boolean,
    isRecentHidden: Boolean,
    onClick: (String) -> Unit,
) {
    items(
        count = list.size,
        key = { list[it].id },
        contentType = { list[it]::class.java },
    ) { index ->
        val item = list[index]
        val title = item.title.resolveReference()

        if (item.isVisible && !isRecentHidden) {
            ListItemWithIcon(
                title = title,
                subtitle = item.subtitle.orMaskWithStars(isBalanceHidden).resolveReference(),
                accountTitleUM = item.accountTitleUM,
                info = item.timestamp?.resolveReference(),
                subtitleEndOffset = item.subtitleEndOffset,
                subtitleIconRes = item.subtitleIconRes,
                onClick = { onClick(title) },
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