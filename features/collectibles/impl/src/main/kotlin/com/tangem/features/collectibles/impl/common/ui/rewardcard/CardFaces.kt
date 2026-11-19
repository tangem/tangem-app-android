package com.tangem.features.collectibles.impl.common.ui.rewardcard

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.res.LocalHapticManager
import kotlin.math.cos

private const val FLIP_DEGREES = 180f
private const val FLIP_DURATION_MS = 420

/** Undoes the mirroring of the back face drawn inside the turning layer. */
private const val BACK_FACE_COUNTER_ROTATION = 180f

private val CARD_CORNER_RADIUS = 12.dp
private val CARD_CORNER = RoundedCornerShape(CARD_CORNER_RADIUS)

private val GLARE_RADIUS = 220.dp
private const val GLARE_PEAK_ALPHA = 0.35f

// Negative cosine means the back faces the camera, for any number of turns
private fun isBackFacing(angle: Float): Boolean = cos(Math.toRadians(angle.toDouble())) < 0.0

/**
 * The card image, turning in place inside its case.
 *
 * @param angle the card's own turn, e.g. the spin it lands with
 * @param isInteractive a tap turns the card over, also before the back face has loaded; a hold tilts it toward
 * the finger under a glare
 */
@Suppress("LongParameterList")
@Composable
internal fun CardFaces(
    angle: Float,
    cameraDistance: Float,
    frontImageUrl: String?,
    backImageUrl: String?,
    isInteractive: Boolean,
    modifier: Modifier = Modifier,
) {
    val hapticManager = LocalHapticManager.current
    var turns by remember { mutableIntStateOf(0) }
    val tilt = rememberCardTiltState()

    Box(
        modifier = modifier.cardGestures(
            isTapEnabled = isInteractive,
            isHoldEnabled = isInteractive,
            onTap = {
                turns++
                hapticManager.perform(TangemHapticEffect.View.SegmentTick)
            },
            onHold = { position, size ->
                if (!tilt.isHeld) hapticManager.perform(TangemHapticEffect.View.ContextClick)
                tilt.hold(position = position, size = size)
            },
            onHoldEnd = tilt::release,
        ),
    ) {
        TurningCard(
            angle = angle,
            turns = turns,
            tilt = tilt,
            cameraDistance = cameraDistance,
            frontImageUrl = frontImageUrl,
            backImageUrl = backImageUrl,
        )

        if (isInteractive) {
            CardGlare(tilt = tilt, cameraDistance = cameraDistance)
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun BoxScope.TurningCard(
    angle: Float,
    turns: Int,
    tilt: CardTiltState,
    cameraDistance: Float,
    frontImageUrl: String?,
    backImageUrl: String?,
) {
    val flipAngle by animateFloatAsState(
        targetValue = turns * FLIP_DEGREES,
        animationSpec = tween(durationMillis = FLIP_DURATION_MS, easing = EaseInOut),
        label = "cardFlip",
    )
    val shownAngle = angle + flipAngle

    Box(
        modifier = Modifier
            .matchParentSize()
            .graphicsLayer {
                rotationX = tilt.rotationX
                rotationY = shownAngle + tilt.rotationY
                this.cameraDistance = cameraDistance
            }
            .clip(CARD_CORNER),
    ) {
        if (isBackFacing(shownAngle)) {
            CardFace(
                imageUrl = backImageUrl,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = BACK_FACE_COUNTER_ROTATION },
            )
        } else {
            CardFace(imageUrl = frontImageUrl, modifier = Modifier.fillMaxSize())
        }
    }
}

/** The light under the finger, tilting with the card but never turned over with it. */
@Composable
private fun BoxScope.CardGlare(tilt: CardTiltState, cameraDistance: Float) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .graphicsLayer {
                rotationX = tilt.rotationX
                rotationY = tilt.rotationY
                alpha = tilt.glare
                this.cameraDistance = cameraDistance
            }
            .clip(CARD_CORNER)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = GLARE_PEAK_ALPHA), Color.Transparent),
                        center = tilt.touch,
                        radius = GLARE_RADIUS.toPx(),
                    ),
                )
            },
    )
}

/** A face whose image is missing or still on its way shimmers in its place. */
@Composable
private fun CardFace(imageUrl: String?, modifier: Modifier = Modifier) {
    var isLoading by remember(imageUrl) { mutableStateOf(true) }

    Box(modifier = modifier) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                onLoading = { isLoading = true },
                onSuccess = { isLoading = false },
                onError = { isLoading = false },
                modifier = Modifier.matchParentSize(),
            )
        }
        if (imageUrl == null || isLoading) {
            TangemShimmer(modifier = Modifier.matchParentSize(), radius = CARD_CORNER_RADIUS)
        }
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