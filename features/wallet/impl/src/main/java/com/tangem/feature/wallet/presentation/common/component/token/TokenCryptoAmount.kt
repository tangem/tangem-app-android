package com.tangem.feature.wallet.presentation.common.component.token

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.impl.R
import com.tangem.utils.StringsSigns
import com.tangem.feature.wallet.presentation.common.state.TokenItemState.CryptoAmountState as TokenCryptoAmountState

@Composable
internal fun TokenCryptoAmount(
    state: TokenCryptoAmountState?,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is TokenCryptoAmountState.Content -> {
            CryptoAmountText(
                amount = if (isBalanceHidden) StringsSigns.STARS else state.text,
                modifier = modifier,
            )
        }
        is TokenCryptoAmountState.Unreachable -> {
            CryptoAmountText(amount = stringResource(id = R.string.common_unreachable), modifier = modifier)
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
private fun CryptoAmountText(amount: String, modifier: Modifier = Modifier) {
    Text(
        text = amount,
        modifier = modifier,
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
