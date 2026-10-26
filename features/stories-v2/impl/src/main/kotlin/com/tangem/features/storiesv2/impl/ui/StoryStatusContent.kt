package com.tangem.features.storiesv2.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.loader.TangemLoaderSize
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import kotlinx.coroutines.delay
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.R as CoreUiR

/**
 * The story is still downloading. Nothing is drawn for a moment first, not even the black frame: a story already on
 * the device — every bundled one — opens within a frame or two, and a black screen flashing for that long over the
 * screen underneath reads as a glitch.
 */
@Composable
internal fun StoryLoadingContent(onCloseClick: () -> Unit, modifier: Modifier = Modifier) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(REVEAL_DELAY_MS)
        isVisible = true
    }

    if (isVisible) {
        StoryStatusFrame(onCloseClick = onCloseClick, modifier = modifier) {
            TangemLoader(size = TangemLoaderSize.X24)
        }
    }
}

@Composable
internal fun StoryErrorContent(onReloadClick: () -> Unit, onCloseClick: () -> Unit, modifier: Modifier = Modifier) {
    StoryStatusFrame(onCloseClick = onCloseClick, modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(TangemTheme.colors3.bg.secondary),
            ) {
                Icon(
                    painter = painterResource(id = CoreUiR.drawable.ic_alert_triangle_20),
                    contentDescription = null,
                    tint = TangemTheme.colors3.icon.secondary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResourceSafe(id = CoreResR.string.common_unable_to_load),
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.primary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResourceSafe(id = CoreResR.string.common_try_again_later),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            TangemButton(
                text = resourceReference(CoreResR.string.common_reload),
                variant = TangemButton.Variant.Secondary,
                size = TangemButton.Size.X9,
                onClick = onReloadClick,
            )
        }
    }
}

/** Black like the story itself, with the close button exactly where the playing story has it. */
@Composable
private fun StoryStatusFrame(
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding(),
    ) {
        Box(modifier = Modifier.align(Alignment.Center)) { content() }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            TangemButton.Close(onClick = onCloseClick, modifier = Modifier.align(Alignment.CenterEnd))
        }
    }
}

private const val REVEAL_DELAY_MS = 300L