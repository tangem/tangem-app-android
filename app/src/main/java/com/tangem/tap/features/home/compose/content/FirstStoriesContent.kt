package com.tangem.tap.features.home.compose.content

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.features.home.compose.StoriesTextAnimation
import com.tangem.wallet.R

@Suppress("LongMethod", "ComplexMethod", "MagicNumber")
@Composable
fun FirstStoriesContent(isPaused: Boolean, duration: Int) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(isPaused) {
        if (isPaused) {
            progress.stop()
        } else {
            progress.animateTo(
                targetValue = 2f,
                animationSpec = tween(
                    durationMillis = duration,
                    easing = LinearEasing,
                ),
            )
        }
    }

    val style = TextStyle(
        fontSize = 46.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.White,
        textAlign = TextAlign.Center,
    )

    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SpacerH(TangemTheme.dimens.spacing94)
        StoriesTextAnimation(
            slideInDuration = 500,
            slideInDelay = 150,
        ) { modifier ->
            Text(
                modifier = modifier,
                text = stringResourceSafe(R.string.story_meet_title),
                style = style,
                color = TangemColorPalette.White,
                textAlign = TextAlign.Center,
            )
        }
        SpacerH(TangemTheme.dimens.spacing46)
        Image(
            modifier = Modifier
                .fillMaxWidth(),
            painter = painterResource(R.drawable.img_meet_tangem),
            contentScale = ContentScale.Inside,
            contentDescription = "Tangem Wallet card",
        )
    }
}

@Preview
@Composable
private fun FirstStoriesPreview() {
    FirstStoriesContent(
        false,
        8000,
    )
}