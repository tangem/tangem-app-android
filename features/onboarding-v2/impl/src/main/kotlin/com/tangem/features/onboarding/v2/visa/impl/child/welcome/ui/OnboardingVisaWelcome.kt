package com.tangem.features.onboarding.v2.visa.impl.child.welcome.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.PrimaryButtonIconEnd
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.visa.impl.child.welcome.ui.state.OnboardingVisaWelcomeUM

@Composable
internal fun OnboardingVisaWelcome(state: OnboardingVisaWelcomeUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier
                .padding(
                    top = 20.dp,
                    bottom = 32.dp,
                    start = 32.dp,
                    end = 32.dp,
                )
                .fillMaxSize()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Artwork()

            SpacerH(50.dp)

            Text(
                text = when (state.mode) {
                    OnboardingVisaWelcomeUM.Mode.Hello -> "Hello, ${state.userName}!"
                    OnboardingVisaWelcomeUM.Mode.WelcomeBack -> "Welcome back, ${state.userName}!"
                },
                style = TangemTheme.typography.h2,
                color = TangemTheme.colors.text.primary1,
            )

            SpacerH12()

            Text(
                text = when (state.mode) {
                    OnboardingVisaWelcomeUM.Mode.Hello -> "bla bla bla //TODO"
                    OnboardingVisaWelcomeUM.Mode.WelcomeBack -> "bla bla bla? //TODO"
                },
                style = TangemTheme.typography.body1,
                color = TangemTheme.colors.text.secondary,
            )
        }

        when (state.mode) {
            OnboardingVisaWelcomeUM.Mode.Hello -> {
                PrimaryButton(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        .fillMaxWidth(),
                    text = "Start Activation",
                    onClick = state.onContinueClick,
                )
            }
            OnboardingVisaWelcomeUM.Mode.WelcomeBack -> {
                PrimaryButtonIconEnd(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        .fillMaxWidth(),
                    text = "Start Activation",
                    iconResId = R.drawable.ic_tangem_24,
                    onClick = state.onContinueClick,
                )
            }
        }
    }
}

@Composable
fun Artwork(modifier: Modifier = Modifier) {
    Box(modifier) {
        val circleColor = TangemTheme.colors.background.secondary

        Canvas(
            modifier = Modifier
                .align(Alignment.Center)
                .size(254.dp)
                .fillMaxSize(),
        ) {
            drawCircle(
                color = circleColor,
                radius = size.width / 2,
                center = center,
            )
        }

        AsyncImage(
            model = R.drawable.img_card_visa,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .size(128.dp), // Adjust the size as needed
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    TangemThemePreview {
        OnboardingVisaWelcome(state = OnboardingVisaWelcomeUM())
    }
}