package com.tangem.feature.wallet.presentation.common.component.token

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemTypography
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.state.TokenItemState

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun TokenTitle(state: TokenItemState, modifier: Modifier = Modifier) {
    AnimatedContent(targetState = state, label = "Update title", modifier = modifier) { animatedState ->
        when (animatedState) {
            is TokenItemState.ContentState -> {
                ContentTitle(
                    name = animatedState.name,
                    hasPending = (animatedState as? TokenItemState.Content)?.hasPending == true,
                )
            }
            is TokenItemState.Loading -> {
                RectangleShimmer(modifier = Modifier.placeholderSize(), radius = TangemTheme.dimens.radius4)
            }
            is TokenItemState.Locked -> {
                LockedRectangle(modifier = Modifier.placeholderSize())
            }
        }
    }
}

@Composable
private fun ContentTitle(name: String, hasPending: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing6)) {
        CurrencyNameText(name = name, modifier = Modifier.weight(weight = 1f, fill = false))

        PendingTransactionImage(
            hasPending = hasPending,
            modifier = Modifier.align(alignment = Alignment.CenterVertically),
        )
    }
}

@Composable
private fun CurrencyNameText(name: String, modifier: Modifier = Modifier) {
    Text(
        text = name,
        modifier = modifier,
        color = TangemTheme.colors.text.primary1,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        style = TangemTypography.subtitle2,
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