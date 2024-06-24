package com.tangem.core.ui.components.inputrow.inner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.components.currency.tokenicon.LoadingIcon
import com.tangem.core.ui.res.TangemTheme

/**
 * Loads image by url for icon in the input row
 *
 * @param imageUrl url of the image
 * @param modifier modifier
 */
@Composable
internal fun InputRowAsyncImage(imageUrl: String, modifier: Modifier = Modifier) {
    SubcomposeAsyncImage(
        modifier = modifier,
        model = ImageRequest.Builder(context = LocalContext.current)
            .data(imageUrl)
            .crossfade(enable = true)
            .allowHardware(enable = false)
            .build(),
        loading = { LoadingIcon() },
        error = {
            Box(
                modifier = Modifier
                    .background(
                        color = TangemTheme.colors.background.tertiary,
                        shape = CircleShape,
                    ),
            )
        },
        contentDescription = null,
    )
}