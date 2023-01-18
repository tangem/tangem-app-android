package com.tangem.feature.referral.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat.startActivity
import com.tangem.core.ui.components.PrimaryStartIconButton
import com.tangem.core.ui.components.SmallInfoCard
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.referral.presentation.R

@Suppress("LongParameterList")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ParticipateBottomBlock(
    purchasedWalletCount: Int,
    code: String,
    shareLink: String,
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
        SmallInfoCard(
            startText = stringResource(id = R.string.referral_friends_bought_title),
            endText = pluralStringResource(
                id = R.plurals.referral_wallets_purchased_count,
                count = purchasedWalletCount,
                purchasedWalletCount,
            ),
        )
        PersonalCodeCard(code = code)
        AdditionalButtons(
            code = code,
            shareLink = shareLink,
            onShowCopySnackbar = onShowCopySnackbar,
            onCopyClick = onCopyClick,
            onShareClick = onShareClick,
        )
        AgreementText(firstPartResId = R.string.referral_tos_enroled_prefix, onClick = onAgreementClick)
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
        Column(Modifier.background(TangemTheme.colors.background.primary)) {
            ParticipateBottomBlock(
                purchasedWalletCount = 3,
                code = "x4JdK",
                shareLink = "",
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
private fun Preview_ParticipateBottomBlock_InDarkTheme() {
    TangemTheme(isDark = true) {
        Column(Modifier.background(TangemTheme.colors.background.primary)) {
            ParticipateBottomBlock(
                purchasedWalletCount = 3,
                code = "x4JdK",
                shareLink = "",
                onAgreementClick = {},
                onShowCopySnackbar = {},
                onCopyClick = {},
                onShareClick = {},
            )
        }
    }
}
