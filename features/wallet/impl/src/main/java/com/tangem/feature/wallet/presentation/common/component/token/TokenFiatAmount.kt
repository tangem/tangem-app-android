package com.tangem.feature.wallet.presentation.common.component.token

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.text.style.TextOverflow
import com.tangem.common.Strings
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemTypography
import com.tangem.feature.wallet.presentation.common.state.TokenItemState

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun TokenFiatAmount(state: TokenItemState, modifier: Modifier = Modifier) {
    AnimatedContent(targetState = state, label = "Update fiat amount", modifier = modifier) { animatedState ->
        when (animatedState) {
            is TokenItemState.Content -> {
                Text(
                    text = if (animatedState.tokenOptions.isBalanceHidden) {
                        Strings.STARS
                    } else {
                        animatedState.tokenOptions.fiatAmount
                    },
                    color = TangemTheme.colors.text.primary1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TangemTypography.body2,
                )
            }
            is TokenItemState.Loading -> {
                RectangleShimmer(modifier = Modifier.placeholderSize(), radius = TangemTheme.dimens.radius4)
            }
            is TokenItemState.Locked -> {
                LockedRectangle(modifier = Modifier.placeholderSize())
            }
            is TokenItemState.Unreachable,
            is TokenItemState.Draggable,
            is TokenItemState.NoAddress,
            -> Unit
        }
    }
}

private fun Modifier.placeholderSize(): Modifier = composed {
    return@composed this
        .padding(vertical = TangemTheme.dimens.spacing4)
        .size(width = TangemTheme.dimens.size40, height = TangemTheme.dimens.size12)
}