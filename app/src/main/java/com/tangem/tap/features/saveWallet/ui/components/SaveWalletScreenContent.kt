package com.tangem.tap.features.saveWallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.components.atoms.Hand
import com.tangem.core.ui.res.TangemTheme
import com.tangem.wallet.R

@Composable
internal fun SaveWalletScreenContent(
    modifier: Modifier = Modifier,
    showProgress: Boolean,
    onSaveWalletClick: () -> Unit,
    onCloseClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .padding(horizontal = TangemTheme.dimens.spacing16),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Hand()
        IconButton(
            modifier = Modifier
                .align(Alignment.End)
                .padding(all = TangemTheme.dimens.spacing8)
                .size(TangemTheme.dimens.size32),
            onClick = onCloseClick,
        ) {
            Icon(
                modifier = Modifier.size(TangemTheme.dimens.size24),
                painter = painterResource(id = R.drawable.ic_close),
                tint = TangemTheme.colors.icon.secondary,
                contentDescription = stringResource(id = R.string.common_cancel),
            )
        }
        Spacer(modifier = Modifier.weight(.7f))
        Icon(
            modifier = Modifier.size(TangemTheme.dimens.size56),
            painter = painterResource(id = R.drawable.ic_fingerprint_24),
            tint = TangemTheme.colors.icon.informative,
            contentDescription = null,
        )
        SpacerH24()
        Box(
            modifier = Modifier
                .background(
                    color = TangemTheme.colors.text.accent.copy(alpha = .12f),
                    shape = TangemTheme.shapes.roundedCornersMedium,
                ),
        ) {
            Text(
                modifier = Modifier
                    .padding(
                        vertical = TangemTheme.dimens.spacing4,
                        horizontal = TangemTheme.dimens.spacing12,
                    ),
                text = stringResource(R.string.save_user_wallet_agreement_new_feature),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.accent,
                textAlign = TextAlign.Center,
            )
        }
        SpacerH24()
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.save_user_wallet_agreement_header),
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
        )
        SpacerH12()
        Text(
            modifier = Modifier.fillMaxWidth(fraction = .87f),
            text = stringResource(R.string.save_user_wallet_agreement_description),
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.weight(1f))
        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            showProgress = showProgress,
            text = stringResource(R.string.save_user_wallet_agreement_allow),
            onClick = onSaveWalletClick,
        )
        SpacerH16()
        Text(
            modifier = Modifier.fillMaxWidth(fraction = .7f),
            text = stringResource(R.string.save_user_wallet_agreement_notice),
            style = TangemTheme.typography.caption,
            color = TangemTheme.colors.text.tertiary,
            textAlign = TextAlign.Center,
        )
        SpacerH16()
    }
}

// region Preview
@Composable
private fun SaveWalletScreenContentSample(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(TangemTheme.colors.background.primary),
    ) {
        SaveWalletScreenContent(
            modifier = Modifier
                .padding(top = TangemTheme.dimens.spacing32)
                .fillMaxSize()
                .background(
                    color = TangemTheme.colors.background.plain,
                    shape = TangemTheme.shapes.bottomSheet,
                ),
            showProgress = false,
            onSaveWalletClick = { /* no-op */ },
            onCloseClick = { /* no-op */ },
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun SaveWalletScreenContentPreview_Light() {
    TangemTheme {
        SaveWalletScreenContentSample()
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun SaveWalletScreenContentPreview_Dark() {
    TangemTheme(isDark = true) {
        SaveWalletScreenContentSample()
    }
}
// endregion Preview