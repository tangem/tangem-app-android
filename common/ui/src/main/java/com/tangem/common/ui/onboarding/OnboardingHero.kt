package com.tangem.common.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
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

/**
 * Full-bleed photo at the top of an onboarding screen, fading into the screen's own background so the headline
 * can sit over its lower edge.
 *
 * The caller places its content over the hero and decides how far down it starts — the hero only draws.
 *
 * @param imageResId    the photo
 * @param frameWidth    width of the design frame the proportions come from
 * @param imageHeight   height of the photo in that frame
 * @param tintStart     where the fade begins, as a fraction of [imageHeight]
 * @param tintEnd       where the fade reaches the background, as a fraction of [imageHeight]
 */
@Composable
fun OnboardingHero(
    @DrawableRes imageResId: Int,
    frameWidth: Float,
    imageHeight: Float,
    tintStart: Float,
    tintEnd: Float,
    modifier: Modifier = Modifier,
) {
    val tintColor = TangemTheme.colors3.bg.primary

    Image(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(frameWidth / imageHeight)
            .drawWithCache {
                val tint = Brush.verticalGradient(
                    colorStops = arrayOf(
                        tintStart to Color.Transparent,
                        tintEnd to tintColor,
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
}