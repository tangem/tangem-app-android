package com.tangem.feature.wallet.presentation.common.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.constraintlayout.compose.ConstrainedLayoutReference
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintLayoutScope
import androidx.constraintlayout.compose.Dimension
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.common.component.token.TokenCryptoInfoBlock
import com.tangem.feature.wallet.presentation.common.component.token.TokenFiatInfoBlock
import com.tangem.feature.wallet.presentation.common.component.token.icon.TokenIcon
import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import org.burnoutcrew.reorderable.ReorderableLazyListState

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TokenItem(
    state: TokenItemState,
    modifier: Modifier = Modifier,
    reorderableTokenListState: ReorderableLazyListState? = null,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val containerModifier: Modifier = remember(state) {
        when (state) {
            is TokenItemState.Content -> modifier.combinedClickable(
                onClick = state.onItemClick,
                onLongClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    state.onItemLongClick()
                },
            )
            else -> modifier
        }
    }
    BaseContainer(modifier = containerModifier) {
        val (iconRef, cryptoInfoRef, fiatInfoRef) = createRefs()

        TokenIcon(
            state = state,
            modifier = Modifier.constrainAs(iconRef) {
                centerVerticallyTo(parent)
                start.linkTo(parent.start)
            },
        )

        TokenCryptoInfoBlock(
            state = state,
            modifier = Modifier
                .padding(horizontal = TangemTheme.dimens.spacing8)
                .constrainAs(cryptoInfoRef) {
                    centerVerticallyTo(parent)
                    start.linkTo(iconRef.end)
                    end.linkTo(fiatInfoRef.start)
                    width = Dimension.fillToConstraints
                },
        )

        TokenFiatInfoBlock(
            state = state,
            modifier = Modifier.constrainAsOptionsItem(scope = this, ref = fiatInfoRef),
            reorderableTokenListState = reorderableTokenListState,
        )
    }
}

@Composable
private inline fun BaseContainer(
    modifier: Modifier = Modifier,
    crossinline content: @Composable ConstraintLayoutScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = TangemTheme.dimens.size68)
            .background(color = TangemTheme.colors.background.primary),
    ) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = TangemTheme.dimens.spacing14,
                    vertical = TangemTheme.dimens.spacing14,
                ),
            content = content,
        )
    }
}

@Stable
private fun Modifier.constrainAsOptionsItem(scope: ConstraintLayoutScope, ref: ConstrainedLayoutReference): Modifier {
    return with(scope) {
        this@constrainAsOptionsItem.constrainAs(ref) {
            centerVerticallyTo(parent)
            end.linkTo(parent.end)
        }
    }
}

// region preview
@Preview
@Composable
private fun Preview_Tokens_LightTheme(@PreviewParameter(TokenConfigProvider::class) state: TokenItemState) {
    TangemTheme(isDark = false) {
        TokenItem(state)
    }
}

@Preview
@Composable
private fun Preview_Tokens_DarkTheme(@PreviewParameter(TokenConfigProvider::class) state: TokenItemState) {
    TangemTheme(isDark = true) {
        TokenItem(state)
    }
}

private class TokenConfigProvider : CollectionPreviewParameterProvider<TokenItemState>(
    collection = listOf(
        WalletPreviewData.tokenItemVisibleState,
        WalletPreviewData.tokenItemUnreachableState,
        WalletPreviewData.tokenItemDragState,
        WalletPreviewData.tokenItemHiddenState,
        WalletPreviewData.loadingTokenItemState,
        WalletPreviewData.testnetTokenItemVisibleState,
        WalletPreviewData.customTokenItemVisibleState,
        WalletPreviewData.customTestnetTokenItemVisibleState,
    ),
)

// endregion preview