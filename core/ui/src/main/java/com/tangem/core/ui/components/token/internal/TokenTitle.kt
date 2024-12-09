package com.tangem.core.ui.components.token.internal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.tangem.core.ui.R
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.components.token.state.TokenItemState.TitleState as TokenTitleState

@Composable
internal fun TokenTitle(state: TokenTitleState?, modifier: Modifier = Modifier) {
    when (state) {
        is TokenTitleState.Content -> {
            ContentTitle(state = state, modifier = modifier)
        }
        is TokenTitleState.Loading -> {
            RectangleShimmer(modifier = modifier.placeholderSize(), radius = TangemTheme.dimens.radius4)
        }
        is TokenTitleState.Locked -> {
            LockedRectangle(modifier = modifier.placeholderSize())
        }
        null -> Unit
    }
}

@Composable
private fun ContentTitle(state: TokenTitleState.Content, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing6),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        /*
         * If currency name has a long width, then it will completely displace the image.
         * So we need to use [weight] to avoid displacement.
         */
        CurrencyNameText(
            name = state.text.resolveReference(),
            isAvailable = state.isAvailable,
            modifier = Modifier.weight(weight = 1f, fill = false),
        )

        PendingTransactionImage(
            hasPending = state.hasPending,
            modifier = Modifier.align(alignment = Alignment.CenterVertically),
        )
    }
}

@Composable
private fun CurrencyNameText(name: String, isAvailable: Boolean, modifier: Modifier = Modifier) {
    Text(
        text = name,
        modifier = modifier,
        color = if (isAvailable) TangemTheme.colors.text.primary1 else TangemTheme.colors.text.tertiary,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        style = TangemTheme.typography.subtitle2,
    )
}

@Composable
private fun PendingTransactionImage(hasPending: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = hasPending, modifier = modifier) {
        Image(
            painter = painterResource(id = R.drawable.img_loader_15),
            contentDescription = null,
        )
    }
}

private fun Modifier.placeholderSize(): Modifier = composed {
    return@composed this
        .padding(vertical = TangemTheme.dimens.spacing4)
        .size(width = TangemTheme.dimens.size70, height = TangemTheme.dimens.size12)
}