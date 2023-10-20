package com.tangem.feature.referral.ui

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
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
import androidx.core.content.ContextCompat.startActivity
import com.tangem.core.ui.components.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.referral.domain.models.ExpectedAward
import com.tangem.feature.referral.domain.models.ExpectedAwards
import com.tangem.feature.referral.presentation.R

@Suppress("LongParameterList")
@OptIn(ExperimentalComposeUiApi::class)
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
        } else if (purchasedWalletCount != 0) {
            EmptyUpcomingPayments()
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
        cornersToRound = if (isExpectedAwardsPresent || purchasedWalletCount != 0) {
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
        startText = stringResource(id = R.string.referral_expected_awards),
        startTextColor = TangemTheme.colors.text.tertiary,
        startTextStyle = TangemTheme.typography.subtitle2,
        endText = pluralStringResource(
            id = R.plurals.referral_number_of_wallets,
            count = expectedAwards.numberOfWallets,
            expectedAwards.numberOfWallets,
        ),
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
private fun EmptyUpcomingPayments() {
    Divider(
        color = TangemTheme.colors.stroke.primary,
        thickness = TangemTheme.dimens.size0_5,
    )
    AwardText(
        startText = stringResource(id = R.string.referral_expected_awards),
        startTextColor = TangemTheme.colors.text.tertiary,
        startTextStyle = TangemTheme.typography.subtitle2,
        endText = "",
        endTextColor = TangemTheme.colors.text.tertiary,
        endTextStyle = TangemTheme.typography.body2,
        cornersToRound = CornersToRound.BOTTOM_2,
    )
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
private fun Preview_ParticipateBottomBlock_InLightTheme() {
    TangemTheme(isDark = false) {
        Column(Modifier.background(TangemTheme.colors.background.secondary)) {
            ParticipateBottomBlock(
                purchasedWalletCount = 3,
                code = "x4JdK",
                shareLink = "",
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
                onAgreementClick = {},
                onShowCopySnackbar = {},
                onCopyClick = {},
                onShareClick = {},
            )
        }
    }
}

@Preview(widthDp = 360, showBackground = true)
@Composable
private fun Preview_ParticipateBottomBlock_Without_Awards_InLightTheme() {
    TangemTheme(isDark = false) {
        Column(Modifier.background(TangemTheme.colors.background.secondary)) {
            ParticipateBottomBlock(
                purchasedWalletCount = 3,
                code = "x4JdK",
                shareLink = "",
                expectedAwards = null,
                onAgreementClick = {},
                onShowCopySnackbar = {},
                onCopyClick = {},
                onShareClick = {},
            )
        }
    }
}

@Preview(widthDp = 360, showBackground = true)
@Composable
private fun Preview_ParticipateBottomBlock_Without_Awards_And_Purchased_Wallets_InLightTheme() {
    TangemTheme(isDark = false) {
        Column(Modifier.background(TangemTheme.colors.background.secondary)) {
            ParticipateBottomBlock(
                purchasedWalletCount = 0,
                code = "x4JdK",
                shareLink = "",
                expectedAwards = null,
                onAgreementClick = {},
                onShowCopySnackbar = {},
                onCopyClick = {},
                onShareClick = {},
            )
        }
    }
}

@Preview(widthDp = 360, showBackground = true)
@Composable
private fun LessMoreButton_White() {
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
private fun Preview_ParticipateBottomBlock_Without_Awards_InDarkTheme() {
    TangemTheme(isDark = true) {
        Column(Modifier.background(TangemTheme.colors.background.secondary)) {
            ParticipateBottomBlock(
                purchasedWalletCount = 3,
                code = "x4JdK",
                shareLink = "",
                expectedAwards = null,
                onAgreementClick = {},
                onShowCopySnackbar = {},
                onCopyClick = {},
                onShareClick = {},
            )
        }
    }
}