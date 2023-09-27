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
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemTypography
import com.tangem.feature.wallet.presentation.common.state.TokenItemState

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun TokenCryptoAmount(state: TokenItemState, modifier: Modifier = Modifier) {
    AnimatedContent(targetState = state, label = "Update crypto amount", modifier = modifier) { animatedState ->
        when (animatedState) {
            is TokenItemState.Content -> {
                CryptoAmountText(
                    amount = if (animatedState.tokenOptions.isBalanceHidden) Strings.STARS else animatedState.amount,
                )
            }
            is TokenItemState.Draggable -> {
                CryptoAmountText(amount = animatedState.info.resolveReference())
            }
            is TokenItemState.Loading -> {
                RectangleShimmer(modifier = Modifier.placeholderSize(), radius = TangemTheme.dimens.radius4)
            }
            is TokenItemState.Locked -> {
                LockedRectangle(modifier = Modifier.placeholderSize())
            }
            is TokenItemState.Unreachable,
            is TokenItemState.NoAddress,
            -> Unit
        }
    }
}

@Composable
private fun CryptoAmountText(amount: String) {
    Text(
        text = amount,
        color = TangemTheme.colors.text.tertiary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TangemTypography.body2,
    )
}

private fun Modifier.placeholderSize(): Modifier = composed {
    return@composed this
        .padding(vertical = TangemTheme.dimens.spacing4)
        .size(width = TangemTheme.dimens.size52, height = TangemTheme.dimens.size12)
}