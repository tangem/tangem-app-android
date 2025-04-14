package com.tangem.features.biometry.impl.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.*
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.biometry.R
import com.tangem.features.biometry.impl.ui.state.AskBiometryUM

@Composable
internal fun AskBiometry(state: AskBiometryUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            if (state.bottomSheetVariant) {
                Header(onCloseClick = state.onDismiss)
            }

            SpacerH32()
            SpacerHHalf()

            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                Title(modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing56))
                SpacerH32()
                Description(
                    modifier = Modifier
                        .padding(horizontal = TangemTheme.dimens.spacing34)
                        .fillMaxWidth(),
                )
            }

            SpacerHHalf()
        }
        Footer(state = state)
        SpacerH16()
    }
}

@Composable
private fun Header(onCloseClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
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
                contentDescription = stringResourceSafe(id = R.string.common_cancel),
            )
        }
        SpacerW8()
    }
}

@Composable
private fun Title(modifier: Modifier = Modifier) {
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
            text = stringResourceSafe(id = R.string.save_user_wallet_agreement_header_biometrics),
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Description(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing24),
        horizontalAlignment = Alignment.Start,
    ) {
        DescriptionItem(
            iconPainter = painterResource(id = R.drawable.ic_face_recognition_24),
            title = stringResourceSafe(id = R.string.save_user_wallet_agreement_access_title),
            description = stringResourceSafe(id = R.string.save_user_wallet_agreement_access_description),
        )
        DescriptionItem(
            iconPainter = painterResource(id = R.drawable.ic_lock_24),
            title = stringResourceSafe(id = R.string.save_user_wallet_agreement_code_title),
            description = stringResourceSafe(id = R.string.save_user_wallet_agreement_code_description_biometrics),
        )
    }
}

@Composable
private fun Footer(state: AskBiometryUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(horizontal = TangemTheme.dimens.spacing16)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            showProgress = state.showProgress,
            text = stringResourceSafe(id = R.string.save_user_wallet_agreement_allow_biometrics),
            onClick = state.onAllowClick,
        )

        if (state.bottomSheetVariant.not()) {
            SpacerH12()

            SecondaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResourceSafe(id = R.string.save_user_wallet_agreement_dont_allow),
                onClick = state.onDontAllowClick,
            )
        }

        SpacerH16()

        Text(
            modifier = Modifier.fillMaxWidth(fraction = .7f),
            text = stringResourceSafe(R.string.save_user_wallet_agreement_notice),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DescriptionItem(iconPainter: Painter, title: String, description: String) {
    Row(
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

@Preview(showBackground = true)
@Composable
private fun Preview() {
    TangemThemePreview {
        AskBiometry(
            state = AskBiometryUM(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewBS() {
    TangemThemePreview {
        AskBiometry(
            state = AskBiometryUM(bottomSheetVariant = true),
        )
    }
}