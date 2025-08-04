package com.tangem.features.hotwallet.setupfinished.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.R
import com.tangem.core.ui.components.FullScreen
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.features.hotwallet.setupfinished.entity.MobileWalletSetupFinishedUM

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MobileWalletSetupFinishedContent(state: MobileWalletSetupFinishedUM, modifier: Modifier = Modifier) {
    val composition by rememberLottieComposition(spec = LottieCompositionSpec.RawRes(R.raw.anim_confetti))
    val progress by animateLottieCompositionAsState(composition)
    var showConfetti by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .background(TangemTheme.colors.background.primary)
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Image(
            painter = painterResource(R.drawable.ic_success_blue_76),
            contentDescription = null,
        )
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 48.dp,
                    top = 20.dp,
                    end = 48.dp,
                ),
            text = stringResourceSafe(R.string.onboarding_done_header),
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
        )
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 48.dp,
                    top = 12.dp,
                    end = 48.dp,
                ),
            text = stringResourceSafe(R.string.backup_complete_description),
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.weight(2f))
        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResourceSafe(R.string.common_continue),
            showProgress = false,
            enabled = true,
            onClick = state.onContinueClick,
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

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewMobileWalletSetupFinishedContent() {
    TangemThemePreview {
        MobileWalletSetupFinishedContent(
            state = MobileWalletSetupFinishedUM(
                onContinueClick = {},
            ),
        )
    }
}