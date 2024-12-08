package com.tangem.features.onboarding.v2.done.impl.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.tangem.core.ui.components.*
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onboarding.v2.impl.R

@Composable
internal fun OnboardingDone(onContinueClick: () -> Unit, modifier: Modifier = Modifier) {
    val composition by rememberLottieComposition(spec = LottieCompositionSpec.RawRes(R.raw.anim_confetti))
    val progress by animateLottieCompositionAsState(composition)
    var showConfetti by remember { mutableStateOf(false) }

    Column(
        modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_success_blue_76),
                tint = Color.Unspecified,
                contentDescription = null,
                modifier = Modifier
                    .size(76.dp),
            )
            SpacerH32()
            Text(
                modifier = Modifier.padding(horizontal = 32.dp),
                text = stringResourceSafe(R.string.onboarding_done_header),
                style = TangemTheme.typography.h2,
                color = TangemTheme.colors.text.primary1,
            )
            SpacerH12()
            Text(
                modifier = Modifier.padding(horizontal = 32.dp),
                text = stringResourceSafe(R.string.onboarding_subtitle_success_tangem_wallet_onboarding),
                style = TangemTheme.typography.body1,
                color = TangemTheme.colors.text.secondary,
                textAlign = TextAlign.Center,
            )
            SpacerH(72.dp)
        }

        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding(),
            text = stringResourceSafe(R.string.onboarding_button_continue_wallet),
            onClick = onContinueClick,
        )
    }

    if (showConfetti) {
        FullScreen(notTouchable = true) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
            )
        }
    }

    LaunchedEffect(Unit) {
        showConfetti = true
    }

    LaunchedEffect(progress == 1f) {
        if (progress == 1f) {
            showConfetti = false
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    TangemThemePreview {
        OnboardingDone(onContinueClick = {})
    }
}