package com.tangem.managetokens.presentation.managetokens.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.components.CircleShimmer
import com.tangem.core.ui.extensions.ImageReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.ImageBackgroundContrastChecker
import com.tangem.features.managetokens.impl.R
import com.tangem.managetokens.presentation.managetokens.state.TokenIconState
import kotlinx.coroutines.launch

@Composable
internal fun TokenIcon(state: TokenIconState, modifier: Modifier = Modifier) {
    val iconModifier = modifier.size(TangemTheme.dimens.size36)
    if (state.iconReference != null) {
        DefaultCurrencyIcon(
            modifier = iconModifier,
            iconReference = state.iconReference,
            errorIcon = {
                PlaceholderIcon(
                    modifier = iconModifier,
                    tint = state.placeholderTint,
                    background = state.placeholderBackground,
                )
            },
        )
    } else {
        PlaceholderIcon(
            modifier = iconModifier,
            tint = state.placeholderTint,
            background = state.placeholderBackground,
        )
    }
}

@Composable
private fun PlaceholderIcon(tint: Color, background: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                color = background,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.matchParentSize(),
            painter = painterResource(id = R.drawable.ic_custom_token_44),
            tint = tint,
            contentDescription = null,
        )
    }
}

@Composable
private inline fun DefaultCurrencyIcon(
    iconReference: ImageReference,
    crossinline errorIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    var iconBackgroundColor by remember { mutableStateOf(Color.Transparent) }
    var isBackgroundColorDefined by remember { mutableStateOf(false) }
    val itemBackgroundColor = TangemTheme.colors.background.primary.toArgb()
    val isDarkTheme = isSystemInDarkTheme()
    val coroutineScope = rememberCoroutineScope()

    val pixelsSize = with(LocalDensity.current) { TangemTheme.dimens.size36.roundToPx() }

    SubcomposeAsyncImage(
        modifier = modifier
            .background(
                color = iconBackgroundColor,
                shape = TangemTheme.shapes.roundedCorners8,
            ),
        model = ImageRequest.Builder(context = LocalContext.current)
            .data(iconReference.getReference())
            .size(size = pixelsSize)
            .memoryCacheKey(key = iconReference.getReference().toString() + pixelsSize)
            .crossfade(enable = true)
            .allowHardware(false)
            .listener(
                onSuccess = { _, result ->
                    if (!isBackgroundColorDefined && isDarkTheme) {
                        coroutineScope.launch {
                            val color = ImageBackgroundContrastChecker(
                                drawable = result.drawable,
                                backgroundColor = itemBackgroundColor,
                                size = pixelsSize,
                            ).getContrastColor(isDarkTheme = true)
                            iconBackgroundColor = color
                            isBackgroundColorDefined = true
                        }
                    }
                },
            )
            .build(),
        loading = { LoadingIcon() },
        error = { errorIcon() },
        contentDescription = null,
    )
}

@Composable
internal fun LoadingIcon(modifier: Modifier = Modifier) {
    CircleShimmer(modifier = modifier)
}
