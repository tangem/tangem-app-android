package com.tangem.features.collectibles.impl.packopening.ui.components.rewardcard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.collectibles.impl.packopening.ui.state.RewardCardUM
import kotlin.math.cos

/** Undoes the mirroring of the back face drawn inside the turning layer. */
private const val BACK_FACE_COUNTER_ROTATION = 180f

/** Tap-to-flip half-turn of the landed card. */
private const val FLIP_MS = 420

private val CARD_CORNER = RoundedCornerShape(12.dp)

// Negative cosine means the back faces the camera, for any number of turns
private fun isBackFacing(angle: Float): Boolean = cos(Math.toRadians(angle.toDouble())) < 0.0

@Composable
internal fun CardFaces(
    angle: Float,
    cameraDistance: Float,
    um: RewardCardUM,
    isFlipEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var flips by remember { mutableIntStateOf(0) }
    val flipAngle by animateFloatAsState(
        targetValue = flips * BACK_FACE_COUNTER_ROTATION,
        animationSpec = tween(FLIP_MS),
        label = "cardFlip",
    )
    val shownAngle = angle + flipAngle

    Box(
        modifier = modifier
            .graphicsLayer {
                rotationY = shownAngle
                this.cameraDistance = cameraDistance
            }
            .clip(CARD_CORNER)
            .then(
                if (isFlipEnabled) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { flips++ },
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        if (isBackFacing(shownAngle)) {
            CardFace(
                label = "BACK",
                imageUrl = um.backImageUrl,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = BACK_FACE_COUNTER_ROTATION },
            )
        } else {
            CardFace(
                label = "FRONT",
                imageUrl = um.frontImageUrl,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun CardFace(label: String, imageUrl: String?, modifier: Modifier = Modifier) {
    if (imageUrl != null) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier,
        )
        return
    }

    Box(
        modifier = modifier.background(color = TangemTheme.colors3.bg.secondary, shape = CARD_CORNER),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = TangemTheme.colors3.text.tertiary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .border(width = 0.5.dp, color = TangemTheme.colors3.border.primary, shape = CARD_CORNER),
        )
    }
}

/** The front face's height to width once it has loaded; null until then or without a front face. */
@Composable
internal fun rememberCardAspect(frontImageUrl: String?): Float? {
    val context = LocalContext.current
    val aspect by produceState<Float?>(initialValue = null, frontImageUrl) {
        val frontUrl = frontImageUrl ?: return@produceState
        val front = context.imageLoader.execute(ImageRequest.Builder(context).data(frontUrl).build()).drawable
        if (front != null && front.intrinsicWidth > 0 && front.intrinsicHeight > 0) {
            value = front.intrinsicHeight.toFloat() / front.intrinsicWidth
        }
    }
    return aspect
}