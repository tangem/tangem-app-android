package com.tangem.core.ui.components.token.internal

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.text.style.TextOverflow
import com.tangem.core.ui.R
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.audits.AuditLabel
import com.tangem.core.ui.components.flicker
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.components.token.state.TokenItemState.Subtitle2State as TokenCryptoAmountState

@Composable
internal fun TokenCryptoAmount(
    state: TokenCryptoAmountState?,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is TokenCryptoAmountState.TextContent -> {
            CryptoAmountText(
                modifier = modifier,
                amount = state.text.orMaskWithStars(isBalanceHidden),
                isFlickering = state.isFlickering,
            )
        }
        is TokenItemState.Subtitle2State.LabelContent -> {
            AuditLabel(state = state.auditLabelUM, modifier = modifier)
        }
        is TokenCryptoAmountState.Unreachable -> {
            CryptoAmountText(
                amount = stringResourceSafe(id = R.string.common_unreachable),
                modifier = modifier,
            )
        }
        is TokenCryptoAmountState.Loading -> {
            RectangleShimmer(modifier = modifier.placeholderSize(), radius = TangemTheme.dimens.radius4)
        }
        is TokenCryptoAmountState.Locked -> {
            LockedRectangle(modifier = modifier.placeholderSize())
        }
        null -> Unit
    }
}

@Composable
private fun CryptoAmountText(amount: String, modifier: Modifier = Modifier, isFlickering: Boolean = false) {
    Text(
        modifier = modifier.flicker(isFlickering),
        text = amount,
        color = TangemTheme.colors.text.tertiary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TangemTheme.typography.caption2,
    )
}

private fun Modifier.placeholderSize(): Modifier = composed {
    return@composed this
        .padding(vertical = TangemTheme.dimens.spacing2)
        .size(width = TangemTheme.dimens.size40, height = TangemTheme.dimens.size12)
}