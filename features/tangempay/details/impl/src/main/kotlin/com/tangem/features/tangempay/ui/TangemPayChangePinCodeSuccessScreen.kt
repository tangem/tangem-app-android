package com.tangem.features.tangempay.ui

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
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.tangempay.details.impl.R

@Composable
internal fun TangemPayChangePinCodeSuccessScreen(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val composition by rememberLottieComposition(spec = LottieCompositionSpec.RawRes(R.raw.anim_confetti))
    val progress by animateLottieCompositionAsState(composition)
    var showConfetti by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        TangemTopAppBar(
            modifier = Modifier.statusBarsPadding(),
            title = resourceReference(R.string.common_done).resolveReference(),
            startButton = null,
            titleAlignment = Alignment.CenterHorizontally,
        )
        Column(
            modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SuccessContent(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            PrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
                    .navigationBarsPadding(),
                text = stringResourceSafe(R.string.common_done),
                onClick = onClick,
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
}

@Composable
private fun SuccessContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
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
            text = stringResourceSafe(R.string.tangempay_card_details_change_pin_success_title),
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
        )
        SpacerH12()
        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = stringResourceSafe(R.string.tangempay_card_details_change_pin_success_description),
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
        )
        SpacerH(72.dp)
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    TangemThemePreview {
        TangemPayChangePinCodeSuccessScreen(onClick = {})
    }
}