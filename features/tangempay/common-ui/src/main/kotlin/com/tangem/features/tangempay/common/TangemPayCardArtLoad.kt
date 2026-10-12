package com.tangem.features.tangempay.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_refresh_20

@Composable
internal fun rememberCardArtRequest(url: String, retryCount: Int): ImageRequest {
    val context = LocalContext.current
    return remember(url, retryCount) {
        ImageRequest.Builder(context)
            .data(url)
            .crossfade(true)
            .setParameter("tangempay_card_art_retry", retryCount, memoryCacheKey = null)
            .diskCachePolicy(if (retryCount > 0) CachePolicy.WRITE_ONLY else CachePolicy.ENABLED)
            .build()
    }
}

@Composable
internal fun TangemPayCardArtError(onRetryClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.secondary),
        contentAlignment = Alignment.Center,
    ) {
        TangemButton(
            variant = TangemButton.Variant.Secondary,
            size = TangemButton.Size.X10,
            iconStart = TangemIconUM.Icon(imageVector = Icons.ic_arrow_refresh_20),
            contentDescription = stringResourceSafe(R.string.common_reload),
            onClick = onRetryClick,
        )
    }
}