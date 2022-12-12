package com.tangem.tap.features.saveWallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.components.SpacerH32
import com.tangem.core.ui.components.SpacerH4
import com.tangem.core.ui.components.SpacerHHalf
import com.tangem.core.ui.components.SpacerW24
import com.tangem.core.ui.components.SpacerW8
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
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Header(
            modifier = Modifier.fillMaxWidth(),
            onCloseClick = onCloseClick,
        )
        SpacerHHalf()
        Title(
            modifier = Modifier
                .widthIn(max = TangemTheme.dimens.size200),
        )
        SpacerH32()
        Description(
            modifier = Modifier
                .padding(
                    start = TangemTheme.dimens.spacing34,
                    end = TangemTheme.dimens.spacing56,
                )
                .fillMaxWidth(),
        )
        SpacerHHalf()
        Footer(
            modifier = Modifier
                .padding(horizontal = TangemTheme.dimens.spacing16)
                .fillMaxWidth(),
            showProgress = showProgress,
            onSaveWalletClick = onSaveWalletClick,
        )
        SpacerH16()
    }
}

@Composable
private fun Header(
    modifier: Modifier = Modifier,
    onCloseClick: () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
    ) {
        Hand()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(
                modifier = Modifier.size(TangemTheme.dimens.size32),
                onClick = onCloseClick,
            ) {
                Icon(
                    modifier = Modifier.size(TangemTheme.dimens.size24),
                    painter = painterResource(id = R.drawable.ic_close_24),
                    tint = TangemTheme.colors.icon.secondary,
                    contentDescription = stringResource(id = R.string.common_cancel),
                )
            }
            SpacerW8()
        }
    }
}

@Composable
private fun Title(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing32),
    ) {
        Icon(
            modifier = Modifier.size(TangemTheme.dimens.size56),
            painter = painterResource(id = R.drawable.ic_fingerprint_24),
            tint = TangemTheme.colors.icon.primary1,
            contentDescription = null,
        )
        Text(
            text = stringResource(id = R.string.save_user_wallet_agreement_header_biometrics),
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Description(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing24),
        horizontalAlignment = Alignment.Start,
    ) {
        DescriptionItem(
            iconPainter = painterResource(id = R.drawable.ic_face_recognition_24),
            title = stringResource(id = R.string.save_user_wallet_agreement_access_title),
            description = stringResource(id = R.string.save_user_wallet_agreement_access_description),
        )
        DescriptionItem(
            iconPainter = painterResource(id = R.drawable.ic_lock_24),
            title = stringResource(id = R.string.save_user_wallet_agreement_code_title),
            description = stringResource(id = R.string.save_user_wallet_agreement_code_description_biometrics),
        )
    }
}

@Composable
private fun Footer(
    modifier: Modifier = Modifier,
    showProgress: Boolean,
    onSaveWalletClick: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing16),
    ) {
        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            showProgress = showProgress,
            text = stringResource(id = R.string.save_user_wallet_agreement_allow_biometrics),
            onClick = onSaveWalletClick,
        )
        Text(
            modifier = Modifier.fillMaxWidth(fraction = .7f),
            text = stringResource(R.string.save_user_wallet_agreement_notice),
            style = TangemTheme.typography.caption,
            color = TangemTheme.colors.text.tertiary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DescriptionItem(
    modifier: Modifier = Modifier,
    iconPainter: Painter,
    title: String,
    description: String,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Icon(
            modifier = Modifier.size(TangemTheme.dimens.size24),
            painter = iconPainter,
            tint = TangemTheme.colors.icon.primary1,
            contentDescription = null,
        )
        SpacerW24()
        Column {
            Text(
                text = title,
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.primary1,
            )
            SpacerH4()
            Text(
                text = description,
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.secondary,
            )
        }
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
