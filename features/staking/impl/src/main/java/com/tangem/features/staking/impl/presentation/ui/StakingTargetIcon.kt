package com.tangem.features.staking.impl.presentation.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.tangem.core.ui.components.inputrow.inner.InputRowAsyncImage
import com.tangem.core.ui.extensions.ImageReference

@Composable
internal fun StakingTargetIcon(
    image: ImageReference?,
    modifier: Modifier = Modifier,
    onImageError: (@Composable () -> Unit)? = null,
) {
    when (image) {
        is ImageReference.Url -> {
            InputRowAsyncImage(
                imageUrl = image.url,
                onImageError = onImageError,
                modifier = modifier,
            )
        }
        is ImageReference.Res -> {
            Image(
                painter = painterResource(image.resId),
                contentDescription = null,
                modifier = modifier,
            )
        }
        null -> {
            onImageError?.invoke()
        }
    }
}