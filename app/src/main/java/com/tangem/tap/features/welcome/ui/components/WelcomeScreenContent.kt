package com.tangem.tap.features.welcome.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.*
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.wallet.R

@Suppress("LongMethod")
@Composable
internal fun WelcomeScreenContent(
    showUnlockProgress: Boolean,
    showScanCardProgress: Boolean,
    onUnlockClick: () -> Unit,
    onScanCardClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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
            text = stringResourceSafe(R.string.welcome_unlock_title),
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
        )
        SpacerH12()
        Text(
            modifier = Modifier
                .padding(horizontal = TangemTheme.dimens.spacing44)
                .fillMaxWidth(),
            text = stringResourceSafe(
                id = R.string.welcome_unlock_description,
                stringResourceSafe(id = R.string.common_biometric_authentication),
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
            text = stringResourceSafe(
                id = R.string.welcome_unlock,
                stringResourceSafe(id = R.string.common_biometrics),
            ),
            showProgress = showUnlockProgress,
            onClick = onUnlockClick,
        )
        SpacerH12()
        PrimaryButtonIconEnd(
            modifier = Modifier
                .padding(horizontal = TangemTheme.dimens.spacing16)
                .fillMaxWidth(),
            text = stringResourceSafe(R.string.welcome_unlock_card),
            showProgress = showScanCardProgress,
            iconResId = R.drawable.ic_tangem_24,
            onClick = onScanCardClick,
        )
        SpacerH16()
    }
}

// region Preview
@Composable
private fun WelcomeScreenContentSample(modifier: Modifier = Modifier) {
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
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WelcomeScreenContentPreview() {
    TangemThemePreview {
        WelcomeScreenContentSample()
    }
}
// endregion Preview