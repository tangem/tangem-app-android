package com.tangem.features.collectibles.impl.machinedetails.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
/**
 * The pack, lit by its own artwork: the same image is drawn twice, blurred underneath and sharp on top.
 *
 * [Modifier.blur] is a no-op below API 31, where the hero degrades to the sharp pack alone.
 */
@Composable
internal fun CollectiblesPackHero(imageUrl: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 48.dp, bottom = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        val model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .crossfade(true)
            .build()

        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(width = 204.dp, height = 294.dp)
                .blur(radius = 48.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded),
        )
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(width = 204.dp, height = 294.dp),
        )
    }
}