package com.tangem.common.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.tangem.core.ui.res.TangemTheme

private const val FRAME_WIDTH = 402f
private const val IMAGE_HEIGHT = 608f
private const val CONTENT_TOP = 495f
private const val DEFAULT_TINT_START_Y = 380f
private const val DEFAULT_TINT_END_Y = 600f

/**
 * Full-bleed photo at the top of an onboarding screen, fading into the screen's own background, with
 * [content] laid out over its lower edge.
 *
 * Sized by aspect ratio rather than a fixed height so the design's proportions hold on every screen width.
 *
 * @param imageResId the photo
 * @param tintStartY y in the design's 608-high image where the fade into the background begins
 * @param tintEndY   y in the design's 608-high image where the fade reaches the solid background
 * @param content    placed over the fade, starting where the design's content column starts
 */
@Composable
fun OnboardingHero(
    @DrawableRes imageResId: Int,
    modifier: Modifier = Modifier,
    tintStartY: Float = DEFAULT_TINT_START_Y,
    tintEndY: Float = DEFAULT_TINT_END_Y,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tintColor = TangemTheme.colors3.bg.primary

    Box(modifier = modifier) {
        Image(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(FRAME_WIDTH / IMAGE_HEIGHT)
                .drawWithCache {
                    val tint = Brush.verticalGradient(
                        colorStops = arrayOf(
                            tintStartY / IMAGE_HEIGHT to Color.Transparent,
                            tintEndY / IMAGE_HEIGHT to tintColor,
                            1f to tintColor,
                        ),
                    )
                    onDrawWithContent {
                        drawContent()
                        drawRect(tint)
                    }
                },
            painter = painterResource(imageResId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
        )

        Column {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(FRAME_WIDTH / CONTENT_TOP),
            )
            content()
        }
    }
}