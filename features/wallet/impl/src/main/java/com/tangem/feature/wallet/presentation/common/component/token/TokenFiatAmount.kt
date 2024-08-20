package com.tangem.feature.wallet.presentation.common.component.token

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.impl.R
import com.tangem.utils.StringsSigns
import com.tangem.feature.wallet.presentation.common.state.TokenItemState.FiatAmountState as TokenFiatAmountState

@Composable
internal fun TokenFiatAmount(state: TokenFiatAmountState?, isBalanceHidden: Boolean, modifier: Modifier = Modifier) {
    when (state) {
        is TokenFiatAmountState.Content -> {
            FiatAmountText(
                text = if (isBalanceHidden) StringsSigns.STARS else state.text,
                hasStaked = state.hasStaked,
                modifier = modifier,
            )
        }
        is TokenFiatAmountState.Loading -> {
            RectangleShimmer(modifier = modifier.placeholderSize(), radius = TangemTheme.dimens.radius4)
        }
        is TokenFiatAmountState.Locked -> {
            LockedRectangle(modifier = modifier.placeholderSize())
        }
        null -> Unit
    }
}

@Composable
private fun FiatAmountText(text: String, hasStaked: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(
            visible = hasStaked,
        ) {
            Icon(
                painter = rememberVectorPainter(image = ImageVector.vectorResource(R.drawable.ic_staking_24)),
                contentDescription = null,
                tint = TangemTheme.colors.icon.accent,
                modifier = Modifier
                    .padding(horizontal = TangemTheme.dimens.spacing4)
                    .size(TangemTheme.dimens.size12),
            )
        }
        Text(
            text = text,
            color = TangemTheme.colors.text.primary1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TangemTheme.typography.body2,
        )
    }
}

private fun Modifier.placeholderSize(): Modifier = composed {
    return@composed this
        .padding(vertical = TangemTheme.dimens.spacing4)
        .size(width = TangemTheme.dimens.size40, height = TangemTheme.dimens.size12)
}
