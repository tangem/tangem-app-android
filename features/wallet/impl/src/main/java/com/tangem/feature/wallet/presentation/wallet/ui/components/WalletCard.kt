package com.tangem.feature.wallet.presentation.wallet.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.tangem.core.ui.components.FontSizeRange
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.ResizableText
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.wallet.state.WalletCardState

private const val DOTS = "•••"

/**
 * Wallet card
 *
 * @param state    state
 * @param modifier modifier
 */
@Composable
internal fun WalletCard(state: WalletCardState, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.defaultMinSize(minHeight = TangemTheme.dimens.size108),
        shape = TangemTheme.shapes.roundedCornersXMedium,
        color = TangemTheme.colors.background.primary,
        onClick = state.onClick ?: {},
        enabled = state.onClick != null,
    ) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TangemTheme.dimens.spacing14),
        ) {
            val (balanceBlock, imageItem) = createRefs()
            Column(
                modifier = Modifier.constrainAs(balanceBlock) {
                    centerVerticallyTo(parent)
                    start.linkTo(parent.start)
                    end.linkTo(imageItem.start)
                    width = Dimension.fillToConstraints
                },
                verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
            ) {
                Title(state)
                Balance(state)
                AdditionalInfo(description = state.additionalInfo)
            }

            val imageWidth = TangemTheme.dimens.size120
            WalletImage(
                id = state.imageResId,
                modifier = Modifier.constrainAs(imageItem) {
                    centerVerticallyTo(parent)
                    top.linkTo(parent.top)
                    end.linkTo(parent.end)
                    height = Dimension.fillToConstraints
                    width = Dimension.value(imageWidth)
                },
            )
        }
    }
}

@Composable
private fun Title(state: WalletCardState) {
    when (state) {
        is WalletCardState.HiddenContent -> {
            Row(horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4)) {
                Text(
                    text = state.title,
                    color = TangemTheme.colors.text.tertiary,
                    style = TangemTheme.typography.body2,
                    maxLines = 1,
                )
                Icon(
                    modifier = Modifier.size(size = TangemTheme.dimens.size20),
                    painter = painterResource(id = R.drawable.ic_eye_off_24),
                    contentDescription = null,
                    tint = TangemTheme.colors.icon.informative,
                )
            }
        }
        else -> {
            Text(
                text = state.title,
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.body2,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun Balance(state: WalletCardState) {
    when (state) {
        is WalletCardState.Content -> {
            ResizableText(
                text = state.balance,
                color = TangemTheme.colors.text.primary1,
                style = TangemTheme.typography.h2,
                fontSizeRange = FontSizeRange(min = 16.sp, max = TangemTheme.typography.h2.fontSize),
                modifier = Modifier.defaultMinSize(minHeight = TangemTheme.dimens.size32),
            )
        }
        is WalletCardState.Loading -> {
            RectangleShimmer(
                modifier = Modifier.size(
                    width = TangemTheme.dimens.size102,
                    height = TangemTheme.dimens.size24,
                ),
            )
        }
        is WalletCardState.HiddenContent -> {
            Text(
                text = DOTS,
                color = TangemTheme.colors.text.primary1,
                style = TangemTheme.typography.h2,
            )
        }
        is WalletCardState.Error -> {
            Text(
                text = "—",
                color = TangemTheme.colors.text.primary1,
                style = TangemTheme.typography.h2,
            )
        }
    }
}

@Composable
private fun AdditionalInfo(description: String) {
    Text(
        text = description,
        color = TangemTheme.colors.text.disabled,
        style = TangemTheme.typography.caption,
    )
}

@Composable
private fun WalletImage(@DrawableRes id: Int, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.FillWidth,
    )
}

// region Preview

@Preview
@Composable
private fun Preview_WalletCard_LightTheme(@PreviewParameter(WalletCardStateProvider::class) state: WalletCardState) {
    TangemTheme(isDark = false) {
        WalletCard(state)
    }
}

@Preview
@Composable
private fun Preview_WalletCard_DarkTheme(@PreviewParameter(WalletCardStateProvider::class) state: WalletCardState) {
    TangemTheme(isDark = true) {
        WalletCard(state)
    }
}

private class WalletCardStateProvider : CollectionPreviewParameterProvider<WalletCardState>(
    collection = listOf(
        WalletPreviewData.walletCardContentState,
        WalletPreviewData.walletCardLoadingState,
        WalletPreviewData.walletCardHiddenContentState,
        WalletPreviewData.walletCardErrorState,
    ),
)

// endregion Preview
