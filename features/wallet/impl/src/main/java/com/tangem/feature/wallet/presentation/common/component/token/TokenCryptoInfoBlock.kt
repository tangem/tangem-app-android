package com.tangem.feature.wallet.presentation.common.component.token

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.res.painterResource
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemTypography
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import com.tangem.feature.wallet.presentation.common.state.TokenItemState.Companion.DOTS
import com.tangem.feature.wallet.presentation.common.state.TokenItemState.TokenOptionsState

@Composable
internal fun TokenCryptoInfoBlock(state: TokenItemState, modifier: Modifier = Modifier) {
    when (state) {
        is TokenItemState.ContentState -> ContentBlock(state = state, modifier = modifier)
        is TokenItemState.Loading -> LoadingBlock(modifier = modifier)
        is TokenItemState.Locked -> LockedBlock(modifier = modifier)
    }
}

@Composable
private fun ContentBlock(state: TokenItemState.ContentState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing2),
    ) {
        CurrencyNameText(
            name = state.name,
            hasPending = (state as? TokenItemState.Content)?.hasPending == true,
        )

        AmountText(
            amount = when (state) {
                is TokenItemState.Content -> if (state.tokenOptions is TokenOptionsState.Hidden) DOTS else state.amount
                is TokenItemState.Draggable -> state.info.resolveReference()
                is TokenItemState.Unreachable -> null
            },
        )
    }
}

@Composable
private fun CurrencyNameText(name: String, hasPending: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8)) {
        Text(
            text = name,
            style = TangemTypography.subtitle2,
            color = TangemTheme.colors.text.primary1,
        )

        PendingTransactionImage(hasPending = hasPending, modifier = Modifier.align(Alignment.CenterVertically))
    }
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

@Composable
private fun AmountText(amount: String?) {
    AnimatedVisibility(visible = !amount.isNullOrBlank()) {
        if (amount == null) return@AnimatedVisibility
        Text(
            text = amount,
            style = TangemTypography.body2,
            color = TangemTheme.colors.text.tertiary,
        )
    }
}

@Composable
private fun LoadingBlock(modifier: Modifier = Modifier) {
    NonContentContainer(modifier = modifier) {
        RectangleShimmer(modifier = Modifier.nameSize(), radius = TangemTheme.dimens.radius4)
        RectangleShimmer(modifier = Modifier.amountSize(), radius = TangemTheme.dimens.radius4)
    }
}

@Composable
private fun LockedBlock(modifier: Modifier = Modifier) {
    NonContentContainer(modifier = modifier) {
        LockedRectangle(modifier = Modifier.nameSize())
        LockedRectangle(modifier = Modifier.amountSize())
    }
}

@Composable
private fun NonContentContainer(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing10),
        content = content,
    )
}

private fun Modifier.nameSize(): Modifier = composed {
    return@composed size(width = TangemTheme.dimens.size72, height = TangemTheme.dimens.size12)
}

private fun Modifier.amountSize(): Modifier = composed {
    return@composed size(width = TangemTheme.dimens.size50, height = TangemTheme.dimens.size12)
}