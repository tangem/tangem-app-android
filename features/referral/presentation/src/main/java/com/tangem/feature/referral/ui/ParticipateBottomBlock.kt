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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat.startActivity
import com.tangem.core.ui.components.PrimaryStartIconButton
import com.tangem.core.ui.components.SmallInfoCard
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TextColorType
import com.tangem.core.ui.res.textColor
import com.tangem.feature.referral.presentation.R

@Composable
internal fun ParticipateBottomBlock(
    purchasedWalletCount: Int,
    code: String,
    shareLink: String,
    onAgreementClicked: () -> Unit,
    showCopySnackbar: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(
                top = dimensionResource(id = R.dimen.spacing24),
                bottom = dimensionResource(id = R.dimen.spacing16),
            )
            .padding(horizontal = dimensionResource(id = R.dimen.spacing16)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing16)),
    ) {
        SmallInfoCard(
            startText = stringResource(id = R.string.referral_friends_bought_title),
            endText = String.format(
                stringResource(id = R.string.referral_wallets_purchased_count),
                purchasedWalletCount,
            ),
        )
        PersonalCodeCard(code = code)
        AdditionalButtons(shareLink = shareLink, showCopySnackbar = showCopySnackbar)
        AgreementText(firstPartResId = R.string.referral_tos_enroled_prefix, onClicked = onAgreementClicked)
    }
}

@Composable
private fun PersonalCodeCard(code: String) {
    Column(
        modifier = Modifier
            .shadow(
                elevation = dimensionResource(id = R.dimen.elevation2),
                shape = RoundedCornerShape(dimensionResource(id = R.dimen.radius12)),
            )
            .background(
                color = MaterialTheme.colors.secondary,
                shape = RoundedCornerShape(dimensionResource(id = R.dimen.radius12)),
            )
            .fillMaxWidth()
            .padding(vertical = dimensionResource(id = R.dimen.spacing12)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing4)),
    ) {
        Text(
            text = stringResource(id = R.string.referral_promo_code_title),
            color = MaterialTheme.colors.textColor(type = TextColorType.TERTIARY),
            maxLines = 1,
            style = MaterialTheme.typography.subtitle2,
        )
        Text(
            text = code,
            color = MaterialTheme.colors.textColor(type = TextColorType.PRIMARY1),
            maxLines = 1,
            style = MaterialTheme.typography.h2,
        )
    }
}

@Composable
private fun AdditionalButtons(shareLink: String, showCopySnackbar: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current
    val hapticFeedback = LocalHapticFeedback.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing16)),
    ) {
        PrimaryStartIconButton(
            modifier = Modifier.weight(1f),
            text = stringResource(id = R.string.common_copy),
            iconResId = R.drawable.ic_copy,
            onClicked = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                clipboardManager.setText(AnnotatedString(shareLink))
                showCopySnackbar()
            },
        )

        val context = LocalContext.current
        PrimaryStartIconButton(
            modifier = Modifier.weight(1f),
            text = stringResource(id = R.string.common_share),
            iconResId = R.drawable.ic_share,
            onClicked = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                context.shareText(shareLink)
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
fun Preview_ParticipateBottomBlock_InLightTheme() {
    TangemTheme(isDark = false) {
        Column(Modifier.background(MaterialTheme.colors.primary)) {
            ParticipateBottomBlock(
                purchasedWalletCount = 3,
                code = "x4JdK",
                shareLink = "",
                onAgreementClicked = {},
                showCopySnackbar = {},
            )
        }
    }
}

@Preview(widthDp = 360, showBackground = true)
@Composable
fun Preview_ParticipateBottomBlock_InDarkTheme() {
    TangemTheme(isDark = true) {
        Column(Modifier.background(MaterialTheme.colors.primary)) {
            ParticipateBottomBlock(
                purchasedWalletCount = 3,
                code = "x4JdK",
                shareLink = "",
                onAgreementClicked = {},
                showCopySnackbar = {},
            )
        }
    }
}