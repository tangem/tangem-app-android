package com.tangem.features.home.impl.ui.compose.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.home.impl.ui.compose.HorizontalSlidingImage
import com.tangem.core.ui.R
import com.tangem.core.ui.utils.dpSize
import com.tangem.core.ui.utils.toPx

@Composable
fun StoriesCurrenciesContent(paused: Boolean, duration: Int) {
    val currencyDrawableList = remember {
        listOf(
            R.drawable.currency0,
            R.drawable.currency1,
            R.drawable.currency2,
            R.drawable.currency3,
            R.drawable.currency4,
        )
    }

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val decreaseRate = remember { 1f / currencyDrawableList.size }
    val designItemHeight = remember { 82.dp }

    BoxWithGradient {
        Column(modifier = Modifier.graphicsLayer(clip = false)) {
            currencyDrawableList.forEachIndexed { index, drawableResId ->
                val painter = painterResource(id = drawableResId)
                val scaledItemSize = scaleToDesignSize(painter.dpSize(), designItemHeight = designItemHeight)
                val itemOversizedScreenWidthBy = scaledItemSize.width - screenWidth
                val moveItemToStartOfScreen = itemOversizedScreenWidthBy / 2

                val chessOffset = if (index.isEven()) 0.dp else scaledItemSize.halfHeight()
                val animateFrom = chessOffset - moveItemToStartOfScreen
                val animateTo = 50.dp - 50.dp * index * decreaseRate

                HorizontalSlidingImage(
                    paused = paused,
                    duration = duration,
                    painter = painter,
                    itemSize = scaledItemSize,
                    startOffset = animateFrom.toPx(),
                    targetOffset = animateTo.toPx(),
                    contentDescription = "Currency row",
                )
                SpacerH12()
            }
        }
    }
}

@Suppress("MagicNumber")
@Composable
fun StoriesWeb3Content(paused: Boolean, duration: Int) {
    val dappsItemList = remember {
        listOf(
            R.drawable.dapps1,
            R.drawable.dapps1,
            R.drawable.dapps2,
            R.drawable.dapps3,
            R.drawable.dapps4,
            R.drawable.dapps5,
        )
    }
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val decreaseRate = remember { 1f / dappsItemList.size }
    val designItemHeight = 75.dp

    BoxWithGradient {
        Column(modifier = Modifier.graphicsLayer(clip = false)) {
            dappsItemList.forEachIndexed { index, drawableResId ->
                val painter = painterResource(id = drawableResId)
                val scaledItemSize = scaleToDesignSize(painter.dpSize(), designItemHeight = designItemHeight)
                val itemOversizedScreenWidthBy = scaledItemSize.width - screenWidth
                val moveItemToStartOfScreen = itemOversizedScreenWidthBy / 2

                val chessOffset = if (index.isEven()) 0.dp else scaledItemSize.width / 3
                val animateFrom = chessOffset - moveItemToStartOfScreen
                val animateTo = 70.dp - 70.dp * index * decreaseRate

                HorizontalSlidingImage(
                    paused = paused,
                    duration = duration,
                    painter = painter,
                    itemSize = scaledItemSize,
                    startOffset = animateFrom.toPx(),
                    targetOffset = animateTo.toPx(),
                    contentDescription = "Web3 row",
                )
            }
        }
    }
}

@Composable
internal fun BoxWithGradient(content: @Composable () -> Unit) {
    val bottomInsetsPx = WindowInsets.navigationBars.getBottom(LocalDensity.current)

    Box(modifier = Modifier.fillMaxSize()) {
        content()
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(TangemTheme.dimens.size164 + bottomInsetsPx.dp)
                .background(BottomGradient),
        )
    }
}

private fun scaleToDesignSize(itemSize: DpSize, designItemHeight: Dp): DpSize {
    val scaleRate = itemSize.height / designItemHeight
    return itemSize / scaleRate
}

private val BottomGradient: Brush = Brush.verticalGradient(
    colors = listOf(
        TangemColorPalette.Black.copy(alpha = 0f),
        TangemColorPalette.Black.copy(alpha = 0.75f),
        TangemColorPalette.Black.copy(alpha = 0.95f),
        TangemColorPalette.Black,
    ),
)

fun DpSize.halfHeight(): Dp = this.height / 2

fun Int.isEven() = this and 1 == 0