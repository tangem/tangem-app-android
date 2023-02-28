package com.tangem.tap.features.home.compose.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH12
import com.tangem.tap.common.compose.extensions.dpSize
import com.tangem.tap.common.compose.extensions.halfHeight
import com.tangem.tap.common.compose.extensions.toPx
import com.tangem.tap.common.extensions.isEven
import com.tangem.tap.features.home.compose.HorizontalSlidingImage
import com.tangem.wallet.R

@Composable
fun StoriesCurrenciesContent(
    paused: Boolean,
    duration: Int,
) {
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

    LightenBox {
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
fun StoriesWeb3Content(
    paused: Boolean,
    duration: Int,
) {
    val dappsItemList = remember {
        listOf(
            R.drawable.dapps0,
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

    LightenBox {
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
private fun LightenBox(content: @Composable () -> Unit) {
    Box {
        content()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 250.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0f),
                            Color.White.copy(alpha = 0.75f),
                            Color.White.copy(alpha = 0.95f),
                            Color.White,
                        ),
                    ),
                ),
        ) {}
    }
}

private fun scaleToDesignSize(itemSize: DpSize, designItemHeight: Dp): DpSize {
    val scaleRate = itemSize.height / designItemHeight
    return itemSize / scaleRate
}
