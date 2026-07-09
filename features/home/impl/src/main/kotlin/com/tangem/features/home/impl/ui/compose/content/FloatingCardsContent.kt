package com.tangem.features.home.impl.ui.compose.content

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import com.tangem.core.ui.R
import com.tangem.core.ui.utils.AnimatedValue
import com.tangem.core.ui.utils.toAnimatable

/**
[REDACTED_AUTHOR]
 */
@Composable
internal fun FloatingCardsContent(isPaused: Boolean, stepDuration: Int) {
    Box(modifier = Modifier.fillMaxSize()) {
        floatingCards.forEach { cardValues ->
            FloatingCardItem(
                isPaused = isPaused,
                imageRes = R.drawable.img_card_placeholder_wallet_2,
                cardValues = cardValues,
                stepDuration = stepDuration,
            )
        }
    }
}

@Composable
private fun FloatingCardItem(
    isPaused: Boolean,
    stepDuration: Int,
    @DrawableRes imageRes: Int,
    cardValues: CardValues,
) {
    Image(
        painter = painterResource(imageRes),
        contentDescription = null,
        modifier = Modifier
            .graphicsLayer(
                translationX = cardValues.translateX.toAnimatable(isPaused, stepDuration).value,
                translationY = cardValues.translateY.toAnimatable(isPaused, stepDuration).value,
                rotationX = cardValues.rotationX.toAnimatable(isPaused, stepDuration).value,
                rotationY = cardValues.rotationY.toAnimatable(isPaused, stepDuration).value,
                rotationZ = cardValues.rotationZ.toAnimatable(isPaused, stepDuration).value,
                scaleX = cardValues.scale.toAnimatable(isPaused, stepDuration).value,
                scaleY = cardValues.scale.toAnimatable(isPaused, stepDuration).value,
            ),
    )
}

private data class CardValues(
    val translateX: AnimatedValue = AnimatedValue(0f, 0f),
    val translateY: AnimatedValue = AnimatedValue(0f, 0f),
    val rotationX: AnimatedValue = AnimatedValue(0f, 0f),
    val rotationY: AnimatedValue = AnimatedValue(0f, 0f),
    val rotationZ: AnimatedValue = AnimatedValue(0f, 0f),
    val scale: AnimatedValue = AnimatedValue(1f, 1f),
)

@Suppress("MagicNumber")
private val floatingCards = listOf(
    CardValues(
        translateX = -400f to -350f,
        translateY = 30f to 32f,
        rotationX = 10f to 15f,
        rotationY = 15f to 15f,
        rotationZ = 40f to 27f,
        scale = 0.6f to 0.6f,
    ),
    CardValues(
        translateX = 350f to 300f,
        translateY = -70f to 0f,
        rotationX = 30f to 48f,
        rotationY = 0f to 5f,
        rotationZ = -34f to -42f,
        scale = 0.47f to 0.35f,
    ),
    CardValues(
        translateX = 320f to 250f,
        translateY = 500f to 500f,
        rotationX = 0f to 3f,
        rotationY = 10f to 10f,
        rotationZ = -45f to -30f,
        scale = 0.6f to 0.75f,
    ),
)