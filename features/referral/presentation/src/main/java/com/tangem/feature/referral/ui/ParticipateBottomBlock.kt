package com.tangem.feature.referral.ui

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.core.content.ContextCompat.startActivity
import com.tangem.core.ui.components.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.referral.domain.models.ExpectedAward
import com.tangem.feature.referral.domain.models.ExpectedAwards
import com.tangem.feature.referral.presentation.R

@Suppress("LongParameterList")
@Composable
internal fun ParticipateBottomBlock(
    purchasedWalletCount: Int,
    code: String,
    shareLink: String,
    expectedAwards: ExpectedAwards?,
    onAgreementClick: () -> Unit,
    onShowCopySnackbar: () -> Unit,
    onCopyClick: () -> Unit,
    onShareClick: () -> Unit,
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
            onShowCopySnackbar = onShowCopySnackbar,
            onCopyClick = onCopyClick,
            onShareClick = onShareClick,
        )
        CounterAndAwards(purchasedWalletCount = purchasedWalletCount, expectedAwards = expectedAwards)
        AgreementText(firstPartResId = R.string.referral_tos_enroled_prefix, onClick = onAgreementClick)
    }
}

@Composable
private fun CounterAndAwards(purchasedWalletCount: Int, expectedAwards: ExpectedAwards?) {
    Column {
        Counter(purchasedWalletCount, expectedAwards)

        if (expectedAwards != null) {
            Awards(expectedAwards)
        }
    }
}

@Composable
private fun Counter(purchasedWalletCount: Int, expectedAwards: ExpectedAwards?) {
    val isExpectedAwardsPresent = expectedAwards != null

    AwardText(
        startText = stringResource(id = R.string.referral_friends_bought_title),
        startTextColor = TangemTheme.colors.text.tertiary,
        startTextStyle = TangemTheme.typography.subtitle2,
        endText = pluralStringResource(
            id = R.plurals.referral_wallets_purchased_count,
            count = purchasedWalletCount,
            purchasedWalletCount,
        ),
        endTextColor = TangemTheme.colors.text.primary1,
        endTextStyle = TangemTheme.typography.body2,
        cornersToRound = if (isExpectedAwardsPresent) {
            CornersToRound.TOP_2
        } else {
            CornersToRound.ALL_4
        },
    )
}

@Suppress("MagicNumber")
@Composable
private fun Awards(expectedAwards: ExpectedAwards) {
    val elementsCountToShowInLessMode = 3
    val isExpanded = remember { mutableStateOf(false) }

    Divider(
        color = TangemTheme.colors.stroke.primary,
        thickness = TangemTheme.dimens.size0_5,
    )
    AwardText(
        startText = if (expectedAwards.expectedAwards.isNotEmpty()) {
            stringResource(id = R.string.referral_expected_awards)
        } else {
            stringResource(id = R.string.referral_no_expected_awards)
        },
        startTextColor = TangemTheme.colors.text.tertiary,
        startTextStyle = TangemTheme.typography.subtitle2,
        endText = if (expectedAwards.expectedAwards.isNotEmpty()) {
            pluralStringResource(
                id = R.plurals.referral_number_of_wallets,
                count = expectedAwards.numberOfWallets,
                expectedAwards.numberOfWallets,
            )
        } else {
            ""
        },
        endTextColor = TangemTheme.colors.text.tertiary,
        endTextStyle = TangemTheme.typography.body2,
        cornersToRound = CornersToRound.ZERO,
    )

    val initialItems = expectedAwards.expectedAwards.take(elementsCountToShowInLessMode)
    val extraItems = expectedAwards.expectedAwards.drop(elementsCountToShowInLessMode)

    initialItems.forEachIndexed { index, expectedAward ->
        AwardText(
            startText = expectedAward.paymentDate,
            startTextColor = TangemTheme.colors.text.primary1,
            startTextStyle = TangemTheme.typography.subtitle2,
            endText = expectedAward.amount,
            endTextColor = TangemTheme.colors.text.primary1,
            endTextStyle = TangemTheme.typography.subtitle2,
            cornersToRound = if (index == initialItems.size - 1 && extraItems.isEmpty()) {
                CornersToRound.BOTTOM_2
            } else {
                CornersToRound.ZERO
            },
        )
    }

    AnimatedVisibility(
        visible = isExpanded.value,
        enter = fadeIn() + expandVertically(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        ExtraItems(extraItems = extraItems)
    }

    if (expectedAwards.expectedAwards.size > elementsCountToShowInLessMode) {
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
                        stringResource(id = R.string.referral_less)
                    } else {
                        stringResource(id = R.string.referral_more)
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
private fun ExtraItems(extraItems: List<ExpectedAward>) {
    Column {
        extraItems.forEach { expectedAward ->
            AwardText(
                startText = expectedAward.paymentDate,
                startTextColor = TangemTheme.colors.text.primary1,
                startTextStyle = TangemTheme.typography.subtitle2,
                endText = expectedAward.amount,
                endTextColor = TangemTheme.colors.text.primary1,
                endTextStyle = TangemTheme.typography.subtitle2,
                cornersToRound = CornersToRound.ZERO,
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
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4),
    ) {
        Text(
            text = stringResource(id = R.string.referral_promo_code_title),
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
    onShowCopySnackbar: () -> Unit,
    onCopyClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    val hapticFeedback = LocalHapticFeedback.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing16),
    ) {
        PrimaryStartIconButton(
            modifier = Modifier.weight(1f),
            text = stringResource(id = R.string.common_copy),
            iconResId = R.drawable.ic_copy_24,
            onClick = {
                onCopyClick.invoke()
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                clipboardManager.setText(AnnotatedString(code))
                onShowCopySnackbar()
            },
        )

        val context = LocalContext.current
        PrimaryStartIconButton(
            modifier = Modifier.weight(1f),
            text = stringResource(id = R.string.common_share),
            iconResId = R.drawable.ic_share_24,
            onClick = {
                onShareClick.invoke()
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                context.shareText(context.getString(R.string.referral_share_link, shareLink))
            },
        )
    }
}

private fun Context.shareText(text: String) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, null)
    startActivity(this, shareIntent, null)
}

@Preview(widthDp = 360, showBackground = true)
@Composable
private fun ParticipateBottomBlockPreview_Light(
    @PreviewParameter(ParticipateBottomBlockDataProvider::class) data: ParticipateBottomBlockData,
) {
    TangemTheme(isDark = false) {
        Column(Modifier.background(TangemTheme.colors.background.secondary)) {
            ParticipateBottomBlock(
                purchasedWalletCount = data.purchasedWalletCount,
                code = data.code,
                shareLink = data.shareLink,
                expectedAwards = data.expectedAwards,
                onAgreementClick = data.onAgreementClick,
                onShowCopySnackbar = data.onShowCopySnackbar,
                onCopyClick = data.onCopyClick,
                onShareClick = data.onShareClick,
            )
        }
    }
}

@Preview(widthDp = 360, showBackground = true)
@Composable
private fun ParticipateBottomBlockPreview_Dark(
    @PreviewParameter(ParticipateBottomBlockDataProvider::class) state: ParticipateBottomBlockData,
) {
    TangemTheme(isDark = true) {
        Column(Modifier.background(TangemTheme.colors.background.secondary)) {
            ParticipateBottomBlock(
                purchasedWalletCount = state.purchasedWalletCount,
                code = state.code,
                shareLink = state.shareLink,
                expectedAwards = state.expectedAwards,
                onAgreementClick = state.onAgreementClick,
                onShowCopySnackbar = state.onShowCopySnackbar,
                onCopyClick = state.onCopyClick,
                onShareClick = state.onShareClick,
            )
        }
    }
}

@Preview(widthDp = 360, showBackground = true)
@Composable
private fun LessMoreButton_Light() {
    TangemTheme(isDark = false) {
        LessMoreButton(
            isExpanded = remember {
                mutableStateOf(false)
            },
        )
    }
}

@Preview(widthDp = 360, showBackground = true)
@Composable
private fun LessMoreButton_Dark() {
    TangemTheme(isDark = true) {
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
    val onShareClick: () -> Unit = {},
)
