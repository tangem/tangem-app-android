package com.tangem.core.ui.components

import android.content.res.Configuration
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

/**
 * Modifier that makes content flicker.
 *
 * @param isFlickering if `true`, content will flicker.
 * @param targetTextAlpha target alpha value for flickering.
 * @param animationDurationMillis duration of flickering animation.
 * */
fun Modifier.flicker(
    isFlickering: Boolean,
    targetTextAlpha: Float = 0.4f,
    animationDurationMillis: Int = 1500,
): Modifier = composed {
    var alphaChange by remember { mutableStateOf(false) }

    val alpha: Float by animateFloatAsState(
        targetValue = if (alphaChange) targetTextAlpha else 1f,
        animationSpec = tween(
            durationMillis = animationDurationMillis,
            easing = CubicBezierEasing(a = 0.45f, b = 0.0f, c = 0.55f, d = 1.0f),
        ),
        finishedListener = {
            alphaChange = it == 1f && isFlickering
        },
    )

    LaunchedEffect(isFlickering) {
        if (isFlickering) {
            alphaChange = true
        }
    }

    this.graphicsLayer {
        this.alpha = alpha
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_FlickerText() {
    TangemThemePreview {
        var isFlickering by remember {
            mutableStateOf(value = true)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = TangemTheme.colors.background.primary)
                .clickable {
                    isFlickering = !isFlickering
                },
        ) {
            Text(
                modifier = Modifier.flicker(isFlickering),
                text = "Flicker text",
                color = TangemTheme.colors.text.primary1,
                style = TangemTheme.typography.body1,
            )
        }
    }
}
// endregion Preview