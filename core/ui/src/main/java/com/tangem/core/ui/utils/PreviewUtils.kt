package com.tangem.core.ui.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import com.tangem.core.ui.components.SpacerH4
import com.tangem.core.ui.res.TangemThemePreview
import kotlinx.coroutines.delay

/**
 * A container that shows a shimmer effect on top of the actual content.
 * The shimmer effect is toggled every 2 seconds.
 * Used for previewing components with shimmer effect and comparing their sizes with the actual content.
 *
 * If height is changing during the preview, it means that the actual content is not aligned with the shimmer effect.
 *
 * @param actualContent The actual content to be displayed.
 * @param shimmerContent The shimmer effect to be displayed.
 */
@Composable
fun PreviewShimmerContainer(actualContent: @Composable () -> Unit, shimmerContent: @Composable () -> Unit) {
    Column {
        var height by remember { mutableIntStateOf(0) }
        Row {
            Text("height = $height")
        }
        SpacerH4()

        var shimmerVisible by remember { mutableStateOf(true) }

        TangemThemePreview {
            Box(
                Modifier.onGloballyPositioned {
                    height = it.size.height
                },
            ) {
                actualContent()
                if (shimmerVisible) {
                    shimmerContent()
                }
            }
        }

        LaunchedEffect(Unit) {
            while (true) {
                delay(timeMillis = 2000)
                shimmerVisible = !shimmerVisible
            }
        }
    }
}
