package com.tangem.feature.wallet.presentation.common.component.token

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.components.CircleShimmer
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.presentation.common.state.TokenItemState

private const val GRAY_SCALE_SATURATION = 0f

@Composable
internal fun TokenIcon(state: TokenItemState, modifier: Modifier = Modifier) {
    when (state) {
        is TokenItemState.ContentState -> ContentIcon(content = state, modifier = modifier)
        is TokenItemState.Loading -> LoadingIcon(modifier = modifier)
        is TokenItemState.Locked -> LockedIcon(modifier = modifier)
    }
}

@Composable
private fun ContentIcon(content: TokenItemState.ContentState, modifier: Modifier = Modifier) {
    BaseContainer(modifier = modifier) {
        val isTestnet = when (content) {
            is TokenItemState.Content -> content.isTestnet
            is TokenItemState.Draggable -> content.isTestnet
            is TokenItemState.Unreachable -> false
        }

        val colorFilter = remember(isTestnet) {
            if (isTestnet) {
                ColorFilter.colorMatrix(
                    colorMatrix = ColorMatrix().apply { setToSaturation(GRAY_SCALE_SATURATION) },
                )
            } else {
                null
            }
        }

        Icon(
            content = content,
            colorFilter = colorFilter,
            modifier = Modifier.align(Alignment.BottomStart),
        )

        NetworkBadge(
            iconResId = content.networkBadgeIconResId,
            colorFilter = colorFilter,
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }
}

@Composable
private fun Icon(content: TokenItemState.ContentState, colorFilter: ColorFilter?, modifier: Modifier = Modifier) {
    val iconUrl = content.tokenIconUrl
    val iconData: Any = remember(iconUrl) {
        if (iconUrl.isNullOrEmpty()) content.tokenIconResId else iconUrl
    }

    SubcomposeAsyncImage(
        modifier = modifier.iconSize(),
        model = ImageRequest.Builder(context = LocalContext.current)
            .data(data = iconData)
            .placeholder(drawableResId = content.tokenIconResId)
            .error(drawableResId = content.tokenIconResId)
            .fallback(drawableResId = content.tokenIconResId)
            .crossfade(enable = true)
            .build(),
        colorFilter = colorFilter,
        contentDescription = null,
    )
}

@Composable
private fun BoxScope.NetworkBadge(
    @DrawableRes iconResId: Int?,
    colorFilter: ColorFilter?,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = iconResId != null,
        modifier = modifier
            .size(TangemTheme.dimens.size18)
            .background(color = TangemTheme.colors.background.primary, shape = CircleShape),
    ) {
        if (iconResId == null) return@AnimatedVisibility

        Image(
            modifier = Modifier
                .padding(all = TangemTheme.dimens.spacing2)
                .align(Alignment.Center),
            painter = painterResource(id = iconResId),
            colorFilter = colorFilter,
            contentDescription = null,
        )
    }
}

@Composable
private fun LoadingIcon(modifier: Modifier = Modifier) {
    BaseContainer(modifier) {
        CircleShimmer(
            modifier = Modifier
                .iconSize()
                .align(alignment = Alignment.BottomStart),
        )
    }
}

@Composable
private fun LockedIcon(modifier: Modifier = Modifier) {
    BaseContainer(modifier) {
        Box(
            modifier = Modifier
                .iconSize()
                .align(Alignment.BottomStart),
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(color = TangemTheme.colors.background.secondary, shape = CircleShape),
            )
        }
    }
}

@Composable
private inline fun BaseContainer(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier = modifier.size(size = TangemTheme.dimens.size40), content = content)
}

private fun Modifier.iconSize(): Modifier = composed {
    return@composed this.size(size = TangemTheme.dimens.size36)
}