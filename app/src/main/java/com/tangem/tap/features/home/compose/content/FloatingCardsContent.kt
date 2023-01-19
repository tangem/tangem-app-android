package com.tangem.tap.features.home.compose.content

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import com.tangem.tap.common.compose.extensions.AnimatedValue
import com.tangem.tap.common.compose.extensions.asImageBitmap
import com.tangem.tap.common.compose.extensions.toAnimatable
import com.tangem.wallet.R

/**
 * Created by Anton Zhilenkov on 08/06/2022.
 */
@Composable
fun FloatingCardsContent(
    isPaused: Boolean,
    stepDuration: Int,
) {
    val imageBitmap = asImageBitmap(R.drawable.card_placeholder_wallet)
    val cards = listOf(
        FloatingCard.first(),
        FloatingCard.second(),
        FloatingCard.third(),
    )
    Box {
        cards.forEach { floatingCard ->
            FloatingCard.Item(
                isPaused = isPaused,
                imageBitmap = imageBitmap,
                cardValues = floatingCard,
                stepDuration = stepDuration,
            )
        }
    }
}

private data class CardValues(
    val translateX: AnimatedValue = AnimatedValue(0f, 0f),
    val translateY: AnimatedValue = AnimatedValue(0f, 0f),
    val rotationX: AnimatedValue = AnimatedValue(0f, 0f),
    val rotationY: AnimatedValue = AnimatedValue(0f, 0f),
    val rotationZ: AnimatedValue = AnimatedValue(0f, 0f),
    val scale: AnimatedValue = AnimatedValue(1f, 1f),
)

private object FloatingCard {

    @Suppress("TopLevelComposableFunctions")
    @Composable
    fun Item(
        isPaused: Boolean,
        stepDuration: Int,
        imageBitmap: ImageBitmap,
        cardValues: CardValues,
    ) {
        Image(
            bitmap = imageBitmap,
            contentDescription = "Floating Tangem card",
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

    @Suppress("MagicNumber")
    fun first(): CardValues = CardValues(
        translateX = -400f to -350f,
        translateY = 30f to 32f,
        rotationX = 10f to 15f,
        rotationY = 15f to 15f,
        rotationZ = 40f to 27f,
        scale = 0.6f to 0.6f,
    )

    @Suppress("MagicNumber")
    fun second(): CardValues = CardValues(
        translateX = 350f to 300f,
        translateY = -70f to 0f,
        rotationX = 30f to 48f,
        rotationY = 0f to 5f,
        rotationZ = -34f to -42f,
        scale = 0.47f to 0.35f,
    )

    @Suppress("MagicNumber")
    fun third(): CardValues = CardValues(
        translateX = 320f to 250f,
        translateY = 500f to 500f,
        rotationX = 0f to 3f,
        rotationY = 10f to 10f,
        rotationZ = -45f to -30f,
        scale = 0.6f to 0.75f,
    )
}
