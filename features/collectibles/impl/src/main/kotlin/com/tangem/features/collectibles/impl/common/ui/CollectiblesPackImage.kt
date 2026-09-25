package com.tangem.features.collectibles.impl.common.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

/**
 * The pack, lit by its own artwork: the same image is drawn twice, blurred underneath and sharp on top.
 *
 * [Modifier.blur] is a no-op below API 31, where the pack is drawn without its glow.
 */
@Composable
internal fun CollectiblesPackImage(
    imageUrl: String?,
    glowRadius: Dp,
    modifier: Modifier = Modifier,
    errorContent: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current
    val model = remember(imageUrl, context) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(true)
            .build()
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = glowRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded),
        )
        if (errorContent == null) {
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            SubcomposeAsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                error = { errorContent() },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}