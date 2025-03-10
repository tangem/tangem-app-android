package com.tangem.feature.referral.ui

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.res.getStringSafe
import com.tangem.core.ui.components.PrimaryButtonIconStart
import com.tangem.core.ui.components.rows.RoundableCornersRow
import com.tangem.core.ui.extensions.pluralStringResourceSafe
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.referral.domain.models.ExpectedAward
import com.tangem.feature.referral.domain.models.ExpectedAwards
import com.tangem.feature.referral.presentation.R
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
@Composable
internal fun ParticipateBottomBlock(
    purchasedWalletCount: Int,
    code: String,
    shareLink: String,
    expectedAwards: ExpectedAwards?,
    snackbarHostState: SnackbarHostState,
    onAgreementClick: () -> Unit,
    onCopyClick: () -> Unit,
    onShareClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(
                top = TangemTheme.dimens.spacing24,
                bottom = TangemTheme.dimens.spacing16,
            )
            .padding(horizontal = TangemTheme.dimens.spacing16),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing16),
    ) {
        PersonalCodeCard(code = code)
        AdditionalButtons(
            code = code,
            shareLink = shareLink,
            snackbarHostState = snackbarHostState,
            onCopyClick = onCopyClick,
            onShareClick = onShareClick,
        )
        CounterAndAwards(purchasedWalletCount = purchasedWalletCount, expectedAwards = expectedAwards)
        AgreementText(firstPartResId = R.string.referral_tos_enroled_prefix, onClick = onAgreementClick)
    }
}

private const val INITIAL_VISIBLE_AWARDS_MAX_COUNT = 3

@Composable
private fun CounterAndAwards(purchasedWalletCount: Int, expectedAwards: ExpectedAwards?) {
    val isExpanded = remember { mutableStateOf(false) }

    val overallItemsCount = calculateOverallItemsCount(
        expectedAwards = expectedAwards,
        isExpanded = isExpanded,
    )

    Column {
        Counter(
            purchasedWalletCount = purchasedWalletCount,
            overallItemsLastIndex = overallItemsCount - 1,
        )

        if (expectedAwards != null) {
            Awards(
                expectedAwards = expectedAwards,
                overallItemsLastIndex = overallItemsCount - 1,
                isExpanded = isExpanded,
            )
        }
    }
}

@Composable
private fun Counter(purchasedWalletCount: Int, overallItemsLastIndex: Int) {
    RoundableCornersRow(
        startText = stringResourceSafe(id = R.string.referral_friends_bought_title),
        startTextColor = TangemTheme.colors.text.tertiary,
        startTextStyle = TangemTheme.typography.subtitle2,
        endText = pluralStringResourceSafe(
            id = R.plurals.referral_wallets_purchased_count,
            count = purchasedWalletCount,
            purchasedWalletCount,
        ),
        endTextColor = TangemTheme.colors.text.primary1,
        endTextStyle = TangemTheme.typography.body2,
        currentIndex = 0,
        lastIndex = overallItemsLastIndex,
    )
}

@Suppress("MagicNumber")
@Composable
private fun Awards(expectedAwards: ExpectedAwards, overallItemsLastIndex: Int, isExpanded: MutableState<Boolean>) {
    HorizontalDivider(
        thickness = TangemTheme.dimens.size0_5,
        color = TangemTheme.colors.stroke.primary,
    )
    RoundableCornersRow(
        startText = if (expectedAwards.expectedAwards.isNotEmpty()) {
            stringResourceSafe(id = R.string.referral_expected_awards)
        } else {
            stringResourceSafe(id = R.string.referral_no_expected_awards)
        },
        startTextColor = TangemTheme.colors.text.tertiary,
        startTextStyle = TangemTheme.typography.subtitle2,
        endText = if (expectedAwards.expectedAwards.isNotEmpty()) {
            pluralStringResourceSafe(
                id = R.plurals.referral_number_of_wallets,
                count = expectedAwards.numberOfWallets,
                expectedAwards.numberOfWallets,
            )
        } else {
            ""
        },
        endTextColor = TangemTheme.colors.text.tertiary,
        endTextStyle = TangemTheme.typography.body2,
        currentIndex = 1,
        lastIndex = overallItemsLastIndex,
    )

    val initialVisibleAwards = expectedAwards.expectedAwards.take(INITIAL_VISIBLE_AWARDS_MAX_COUNT)
    val initialHiddenAwards = expectedAwards.expectedAwards.drop(INITIAL_VISIBLE_AWARDS_MAX_COUNT)

    initialVisibleAwards.forEachIndexed { index, expectedAward ->
        RoundableCornersRow(
            startText = expectedAward.paymentDate,
            startTextColor = TangemTheme.colors.text.primary1,
            startTextStyle = TangemTheme.typography.subtitle2,
            endText = expectedAward.amount,
            endTextColor = TangemTheme.colors.text.primary1,
            endTextStyle = TangemTheme.typography.subtitle2,
            currentIndex = index + 2,
            lastIndex = overallItemsLastIndex,
        )
    }

    AnimatedVisibility(
        visible = isExpanded.value,
        enter = fadeIn() + expandVertically(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        ExtraItems(
            extraItems = initialHiddenAwards,
            overallItemsLastIndex = overallItemsLastIndex,
            startIndex = initialVisibleAwards.size + 2,
        )
    }

    if (initialHiddenAwards.isNotEmpty()) {
        LessMoreButton(isExpanded = isExpanded)
    }
}

@Composable
private fun LessMoreButton(isExpanded: MutableState<Boolean>) {
    Surface(
        shape = RoundedCornerShape(
            bottomStart = TangemTheme.dimens.radius12,
            bottomEnd = TangemTheme.dimens.radius12,
        ),
    ) {
        Column(
            modifier = Modifier.background(TangemTheme.colors.background.primary),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(TangemTheme.dimens.size48)
                    .clickable { isExpanded.value = !isExpanded.value }
                    .padding(
                        horizontal = TangemTheme.dimens.spacing16,
                        vertical = TangemTheme.dimens.spacing12,
                    ),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isExpanded.value) {
                        stringResourceSafe(id = R.string.referral_less)
                    } else {
                        stringResourceSafe(id = R.string.referral_more)
                    },
                    color = TangemTheme.colors.text.tertiary,
                    style = TangemTheme.typography.subtitle2,
                )

                val chevronIcon = if (isExpanded.value) {
                    painterResource(id = com.tangem.core.ui.R.drawable.ic_chevron_up_24)
                } else {
                    painterResource(id = com.tangem.core.ui.R.drawable.ic_chevron_24)
                }
                Icon(
                    modifier = Modifier.size(TangemTheme.dimens.size20),
                    painter = chevronIcon,
                    tint = TangemTheme.colors.text.tertiary,
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
private fun ExtraItems(extraItems: List<ExpectedAward>, overallItemsLastIndex: Int, startIndex: Int) {
    Column {
        extraItems.forEachIndexed { index, expectedAward ->
            RoundableCornersRow(
                startText = expectedAward.paymentDate,
                startTextColor = TangemTheme.colors.text.primary1,
                startTextStyle = TangemTheme.typography.subtitle2,
                endText = expectedAward.amount,
                endTextColor = TangemTheme.colors.text.primary1,
                endTextStyle = TangemTheme.typography.subtitle2,
                currentIndex = index + startIndex,
                lastIndex = overallItemsLastIndex,
            )
        }
    }
}

@Composable
private fun PersonalCodeCard(code: String) {
    Column(
        modifier = Modifier
            .shadow(
                elevation = TangemTheme.dimens.elevation2,
                shape = RoundedCornerShape(TangemTheme.dimens.radius12),
            )
            .background(
                color = TangemTheme.colors.background.secondary,
                shape = RoundedCornerShape(TangemTheme.dimens.radius12),
            )
            .fillMaxWidth()
            .padding(vertical = TangemTheme.dimens.spacing12),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
    ) {
        Text(
            text = stringResourceSafe(id = R.string.referral_promo_code_title),
            color = TangemTheme.colors.text.tertiary,
            maxLines = 1,
            style = TangemTheme.typography.subtitle2,
        )
        Text(
            text = code,
            color = TangemTheme.colors.text.primary1,
            maxLines = 1,
            style = TangemTheme.typography.h2,
        )
    }
}

@Composable
private fun AdditionalButtons(
    code: String,
    shareLink: String,
    snackbarHostState: SnackbarHostState,
    onCopyClick: () -> Unit,
    onShareClick: (String) -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    val hapticFeedback = LocalHapticFeedback.current

    val coroutineScope = rememberCoroutineScope()
    val resources = LocalContext.current.resources

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing16),
    ) {
        PrimaryButtonIconStart(
            text = stringResourceSafe(id = R.string.common_copy),
            iconResId = R.drawable.ic_copy_24,
            onClick = {
                onCopyClick.invoke()
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                clipboardManager.setText(AnnotatedString(code))

                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = resources.getStringSafe(R.string.referral_promo_code_copied),
                        duration = SnackbarDuration.Short,
                    )
                }
            },
            modifier = Modifier.weight(1f),
        )

        val context = LocalContext.current
        PrimaryButtonIconStart(
            text = stringResourceSafe(id = R.string.common_share),
            iconResId = R.drawable.ic_share_24,
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onShareClick(context.getString(R.string.referral_share_link, shareLink))
            },
            modifier = Modifier.weight(1f),
        )
    }
}

private fun calculateOverallItemsCount(expectedAwards: ExpectedAwards?, isExpanded: MutableState<Boolean>): Int {
    val totalItems = expectedAwards?.expectedAwards?.size ?: 0
    val initialItemsCount = minOf(totalItems, INITIAL_VISIBLE_AWARDS_MAX_COUNT)
    val extraItemsCount = maxOf(0, totalItems - INITIAL_VISIBLE_AWARDS_MAX_COUNT)

    val awardItems = when {
        expectedAwards == null -> 0
        extraItemsCount == 0 -> 1 + initialItemsCount
        isExpanded.value -> 1 + initialItemsCount + extraItemsCount + 1
        else -> 1 + initialItemsCount + 1
    }

    val counterItems = 1
    return counterItems + awardItems
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ParticipateBottomBlockPreview(
    @PreviewParameter(ParticipateBottomBlockDataProvider::class) data: ParticipateBottomBlockData,
) {
    TangemThemePreview {
        Column(Modifier.background(TangemTheme.colors.background.secondary)) {
            ParticipateBottomBlock(
                purchasedWalletCount = data.purchasedWalletCount,
                code = data.code,
                shareLink = data.shareLink,
                expectedAwards = data.expectedAwards,
                onAgreementClick = data.onAgreementClick,
                snackbarHostState = SnackbarHostState(),
                onCopyClick = data.onCopyClick,
                onShareClick = data.onShareClick,
            )
        }
    }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LessMoreButtonPreview() {
    TangemThemePreview {
        LessMoreButton(
            isExpanded = remember {
                mutableStateOf(false)
            },
        )
    }
}

private class ParticipateBottomBlockDataProvider : CollectionPreviewParameterProvider<ParticipateBottomBlockData>(
    collection = listOf(
        ParticipateBottomBlockData(
            purchasedWalletCount = 3,
            expectedAwards = ExpectedAwards(
                numberOfWallets = 3,
                expectedAwards = listOf(
                    ExpectedAward(
                        amount = "10 USDT",
                        paymentDate = "Today",
                    ),
                    ExpectedAward(
                        amount = "20 USDT",
                        paymentDate = "6 Aug 2023",
                    ),
                    ExpectedAward(
                        amount = "30 USDT",
                        paymentDate = "10 Aug 2023",
                    ),
                ),
            ),
        ),
        ParticipateBottomBlockData(
            purchasedWalletCount = 3,
            expectedAwards = ExpectedAwards(
                numberOfWallets = 3,
                expectedAwards = emptyList(),
            ),
        ),
        ParticipateBottomBlockData(
            purchasedWalletCount = 3,
            expectedAwards = null,
        ),
        ParticipateBottomBlockData(
            purchasedWalletCount = 0,
            expectedAwards = null,
        ),
    ),
)

private data class ParticipateBottomBlockData(
    val purchasedWalletCount: Int,
    val expectedAwards: ExpectedAwards?,
    val code: String = "x4JDK",
    val shareLink: String = "",
    val onAgreementClick: () -> Unit = {},
    val onShowCopySnackbar: () -> Unit = {},
    val onCopyClick: () -> Unit = {},
    val onShareClick: (String) -> Unit = {},
)