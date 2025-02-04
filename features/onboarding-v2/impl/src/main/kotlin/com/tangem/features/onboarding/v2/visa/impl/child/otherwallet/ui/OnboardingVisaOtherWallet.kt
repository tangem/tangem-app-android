package com.tangem.features.onboarding.v2.visa.impl.child.otherwallet.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.visa.impl.child.otherwallet.ui.state.OnboardingVisaOtherWalletUM

@Composable
internal fun OnboardingVisaOtherWallet(state: OnboardingVisaOtherWalletUM, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(top = 12.dp, bottom = 32.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                IconBlock()

                SpacerH12()

                Text(
                    text = "Go to Website",
                    style = TangemTheme.typography.h2,
                    color = TangemTheme.colors.text.primary1,
                    textAlign = TextAlign.Center,
                )

                SpacerH12()

                Text(
                    text = "You will be able to complete your connection \n" +
                        "on the third-party web-site \n" +
                        "and back to the Tangem app",
                    style = TangemTheme.typography.body1,
                    color = TangemTheme.colors.text.secondary,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Column {
            PrimaryButton(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                text = "Open in Browser",
                onClick = state.onOpenInBrowserClick,
            )

            SpacerH12()

            SecondaryButton(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .fillMaxWidth(),
                text = "Share Link",
                onClick = state.onOpenInBrowserClick,
            )
        }
    }
}

@Composable
private fun IconBlock(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(TangemTheme.colors.button.primary, TangemTheme.shapes.roundedCorners8),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_tangem_24),
                contentDescription = null,
                tint = TangemTheme.colors.icon.primary2,
                modifier = Modifier.size(38.dp),
            )
        }

        Image(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_loader),
            contentDescription = null,
            modifier = Modifier.width(32.dp),
        )

        Box(
            modifier = Modifier
                .size(64.dp)
                .background(TangemTheme.colors.background.tertiary, TangemTheme.shapes.roundedCorners8),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_web_24),
                contentDescription = null,
                tint = TangemTheme.colors.icon.primary1,
                modifier = Modifier.size(48.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    TangemThemePreview {
        OnboardingVisaOtherWallet(OnboardingVisaOtherWalletUM())
    }
}