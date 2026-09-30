package com.tangem.features.collectibles.impl.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_error_20_filled

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
    errorContent: @Composable () -> Unit = { CollectiblesPackImageErrorIcon() },
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
        SubcomposeAsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            error = { errorContent() },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
internal fun CollectiblesPackImageErrorIcon(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        TangemIcon(
            tangemIconUM = TangemIconUM.Icon(
                imageVector = Icons.ic_error_20_filled,
                tintReference = { TangemTheme.colors3.icon.tertiary },
            ),
            modifier = Modifier
                .size(40.dp)
                .background(color = TangemTheme.colors3.bg.opaque.primary, shape = CircleShape)
                .padding(10.dp)
                .size(20.dp),
        )
    }
}

/** The error icon on a card of its own, for screens where nothing behind the pack outlines it. */
@Composable
internal fun CollectiblesPackImageErrorCard() {
    CollectiblesPackImageErrorIcon(
        modifier = Modifier
            .fillMaxSize()
            .background(color = TangemTheme.colors3.bg.secondary, shape = RoundedCornerShape(20.dp)),
    )
}