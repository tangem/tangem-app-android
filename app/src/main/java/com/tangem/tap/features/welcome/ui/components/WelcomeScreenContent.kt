package com.tangem.tap.features.welcome.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.PrimaryButtonIconRight
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.components.SpacerH32
import com.tangem.core.ui.components.SpacerHMax
import com.tangem.core.ui.res.TangemTheme
import com.tangem.wallet.R

@Suppress("LongMethod")
@Composable
internal fun WelcomeScreenContent(
    showUnlockProgress: Boolean,
    showScanCardProgress: Boolean,
    onUnlockClick: () -> Unit,
    onScanCardClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SpacerHMax()
        Icon(
            modifier = Modifier.size(TangemTheme.dimens.size96),
            painter = painterResource(id = R.drawable.img_tangem_logo_96),
            tint = TangemTheme.colors.icon.primary1,
            contentDescription = null,
        )
        SpacerH32()
        Text(
            modifier = Modifier
                .padding(horizontal = TangemTheme.dimens.spacing16)
                .fillMaxWidth(),
            text = stringResource(R.string.welcome_unlock_title),
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
        )
        SpacerH12()
        Text(
            modifier = Modifier
                .padding(horizontal = TangemTheme.dimens.spacing44)
                .fillMaxWidth(),
            text = stringResource(
                id = R.string.welcome_unlock_description,
                stringResource(id = R.string.common_biometric_authentication),
            ),
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
        )
        SpacerHMax()
        SecondaryButton(
            modifier = Modifier
                .padding(horizontal = TangemTheme.dimens.spacing16)
                .fillMaxWidth(),
            text = stringResource(
                id = R.string.welcome_unlock,
                stringResource(id = R.string.common_biometrics),
            ),
            showProgress = showUnlockProgress,
            onClick = onUnlockClick,
        )
        SpacerH12()
        PrimaryButtonIconRight(
            modifier = Modifier
                .padding(horizontal = TangemTheme.dimens.spacing16)
                .fillMaxWidth(),
            text = stringResource(R.string.welcome_unlock_card),
            showProgress = showScanCardProgress,
            icon = painterResource(id = R.drawable.ic_tangem_24),
            onClick = onScanCardClick,
        )
        SpacerH16()
    }
}

// region Preview
@Composable
private fun WelcomeScreenContentSample(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(TangemTheme.colors.background.primary),
    ) {
        WelcomeScreenContent(
            showUnlockProgress = false,
            showScanCardProgress = false,
            onUnlockClick = { /* no-op */ },
            onScanCardClick = { /* no-op */ },
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun WelcomeScreenContentPreview_Light() {
    TangemTheme {
        WelcomeScreenContentSample()
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun WelcomeScreenContentPreview_Dark() {
    TangemTheme(isDark = true) {
        WelcomeScreenContentSample()
    }
}
// endregion Preview
