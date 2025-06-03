package com.tangem.features.onboarding.v2.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.components.artwork.ArtworkUM
import com.tangem.features.onboarding.v2.impl.R

@Composable
internal fun WalletCard(artwork: ArtworkUM?, modifier: Modifier = Modifier) {
    AsyncImage(
        modifier = modifier,
        model = ImageRequest.Builder(LocalContext.current)
            .data(artwork?.verifiedArtwork?.toByteArray() ?: artwork?.defaultUrl)
            .crossfade(true)
            .build(),
        placeholder = painterResource(R.drawable.card_placeholder_black),
        error = painterResource(R.drawable.card_placeholder_black),
        fallback = painterResource(R.drawable.card_placeholder_black),
        contentScale = ContentScale.Fit,
        contentDescription = null,
    )
}