package com.tangem.common.ui.swapStoriesScreen

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.tangem.core.ui.components.SystemBarsIconsDisposable
import com.tangem.core.ui.components.stories.StoriesContainer
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.LocalWindowSize
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import kotlinx.collections.immutable.persistentListOf

private val SubtitleColor = Color(0xFFB0B0B0)
private const val STORIES_RELATIVE_PADDING = 0.7

@Composable
fun SwapStoriesScreen(config: SwapStoriesUM) {
    if (config !is SwapStoriesUM.Content) return

    SystemBarsIconsDisposable(darkIcons = false)

    StoriesContainer(
        config = config,
    ) { current, _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TangemColorPalette.Black),
        ) {
            SubcomposeAsyncImage(
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                model = ImageRequest.Builder(context = LocalContext.current)
                    .data(current.imageUrl)
                    .crossfade(enable = false)
                    .allowHardware(true)
                    .memoryCacheKey(current.imageUrl)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
                loading = { },
                error = { },
                contentDescription = null,
            )
            SwapStoriesText(current)
        }
    }
}

@Composable
private fun SwapStoriesText(current: SwapStoriesUM.Content.Config) {
    val height = LocalWindowSize.current.height.value
    val textAlign = (height.dp.value * STORIES_RELATIVE_PADDING).dp
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(
                top = textAlign,
                start = 44.dp,
                end = 44.dp,
            ),
    ) {
        Text(
            text = current.title.resolveReference(),
            style = TextStyle(
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = TextUnit(value = 0.1f, type = TextUnitType.Sp),
                lineHeight = TextUnit(value = 34f, type = TextUnitType.Sp),
            ),
            color = TangemTheme.colors.text.constantWhite,
            textAlign = TextAlign.Center,
        )
        Text(
            text = current.subtitle.resolveReference(),
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = TextUnit(value = 0.1f, type = TextUnitType.Sp),
                lineHeight = TextUnit(value = 20f, type = TextUnitType.Sp),
            ),
            color = SubtitleColor,
            textAlign = TextAlign.Center,
        )
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Preview(showBackground = true, widthDp = 360, heightDp = 720, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SwapStoriesScreen_Preview() {
    TangemThemePreview {
        SwapStoriesScreen(
            SwapStoriesUM.Content(
                stories = persistentListOf(
                    SwapStoriesUM.Content.Config(
                        imageUrl = "https://devweb.tangem.com/images/stories/swap/image1.png",
                        title = stringReference("Exchange With Us"),
                        subtitle = stringReference(
                            "Trusted exchange providers let you swap assets effortlessly",
                        ),
                    ),
                ),
                onClose = {},
            ),
        )
    }
}
// endregion