package com.tangem.feature.wallet.presentation.wallet.ui.components.common

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.constraintlayout.compose.ConstraintLayoutScope
import androidx.constraintlayout.compose.Dimension
import com.tangem.core.ui.components.FontSizeRange
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.ResizableText
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemDimens
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletCardState

/**
 * Wallet card
 *
 * @param state    state
 * @param modifier modifier
 *
[REDACTED_AUTHOR]
 */
@Composable
internal fun WalletCard(state: WalletCardState, modifier: Modifier = Modifier) {
    @Suppress("DestructuringDeclarationWithTooManyEntries")
    CardContainer(onClick = state.onClick, modifier = modifier) {
        val (title, balance, additionalText, image) = createRefs()

        val contentVerticalMargin = TangemTheme.dimens.spacing12
        Title(
            state = state,
            modifier = Modifier.constrainAs(title) {
                start.linkTo(parent.start)
                top.linkTo(anchor = parent.top, margin = contentVerticalMargin)
                end.linkTo(image.start)
                width = Dimension.fillToConstraints
            },
        )

        val betweenContentMargin = TangemTheme.dimens.spacing8
        Balance(
            state = state,
            modifier = Modifier.constrainAs(balance) {
                start.linkTo(parent.start)
                top.linkTo(anchor = title.bottom, margin = betweenContentMargin)
                bottom.linkTo(anchor = additionalText.top, margin = betweenContentMargin)
            },
        )

        AdditionalInfo(
            state = state,
            modifier = Modifier.constrainAs(additionalText) {
                start.linkTo(parent.start)
                bottom.linkTo(anchor = parent.bottom, margin = contentVerticalMargin)
            },
        )

        val imageWidth = TangemTheme.dimens.size120
        Image(
            id = state.imageResId,
            modifier = Modifier.constrainAs(image) {
                centerVerticallyTo(parent)
                top.linkTo(parent.top)
                end.linkTo(parent.end)
                height = Dimension.fillToConstraints
                width = Dimension.value(imageWidth)
            },
        )
    }
}

@Composable
private fun CardContainer(
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable ConstraintLayoutScope.() -> Unit,
) {
    Surface(
        modifier = modifier.defaultMinSize(minHeight = TangemTheme.dimens.size108),
        shape = TangemTheme.shapes.roundedCornersXMedium,
        color = TangemTheme.colors.background.primary,
        onClick = onClick ?: {},
        enabled = onClick != null,
    ) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TangemTheme.dimens.spacing14),
        ) {
            content()
        }
    }
}

@Composable
private fun Title(state: WalletCardState, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4),
    ) {
        TitleText(title = state.title)

        AnimatedVisibility(visible = state is WalletCardState.HiddenContent, label = "Update the hidden icon") {
            Icon(
                modifier = Modifier.size(size = TangemTheme.dimens.size20),
                painter = painterResource(id = R.drawable.ic_eye_off_24),
                contentDescription = null,
                tint = TangemTheme.colors.icon.informative,
            )
        }
    }
}

@Composable
private fun TitleText(title: String) {
    Text(
        text = title,
        color = TangemTheme.colors.text.tertiary,
        style = TangemTheme.typography.body2,
        maxLines = 1,
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun Balance(state: WalletCardState, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = state,
        label = "Update the balance",
        modifier = modifier,
    ) { walletCardState ->
        when (walletCardState) {
            is WalletCardState.Content -> {
                ResizableText(
                    text = walletCardState.balance,
                    fontSizeRange = FontSizeRange(min = 16.sp, max = TangemTheme.typography.h2.fontSize),
                    modifier = Modifier.defaultMinSize(minHeight = TangemTheme.dimens.size32),
                    color = TangemTheme.colors.text.primary1,
                    style = TangemTheme.typography.h2,
                )
            }
            is WalletCardState.HiddenContent -> NonContentBalanceText(text = WalletCardState.HIDDEN_BALANCE_TEXT)
            is WalletCardState.Error -> NonContentBalanceText(text = WalletCardState.EMPTY_BALANCE_TEXT)
            is WalletCardState.Loading -> {
                RectangleShimmer(modifier = Modifier.nonContentBalanceSize(TangemTheme.dimens))
            }
            is WalletCardState.LockedContent -> {
                LockedContent(modifier = Modifier.nonContentBalanceSize(TangemTheme.dimens))
            }
        }
    }
}

@Composable
private fun NonContentBalanceText(text: TextReference) {
    Text(
        text = text.resolveReference(),
        color = TangemTheme.colors.text.primary1,
        style = TangemTheme.typography.h2,
    )
}

private fun Modifier.nonContentBalanceSize(dimens: TangemDimens): Modifier {
    return size(width = dimens.size102, height = dimens.size32)
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AdditionalInfo(state: WalletCardState, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = state.additionalInfo,
        label = "Update the additional text",
        modifier = modifier,
    ) { additionalInfo ->
        if (additionalInfo != null) {
            AdditionalInfoText(text = additionalInfo)
        } else {
            when (state) {
                is WalletCardState.Loading -> {
                    RectangleShimmer(modifier = Modifier.nonContentAdditionalInfoSize(TangemTheme.dimens))
                }
                is WalletCardState.LockedContent -> {
                    LockedContent(modifier = Modifier.nonContentAdditionalInfoSize(TangemTheme.dimens))
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun AdditionalInfoText(text: TextReference) {
    Text(
        text = text.resolveReference(),
        color = TangemTheme.colors.text.disabled,
        style = TangemTheme.typography.caption,
    )
}

private fun Modifier.nonContentAdditionalInfoSize(dimens: TangemDimens): Modifier {
    return size(width = dimens.size84, height = dimens.size16)
}

@Composable
private fun LockedContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            color = TangemTheme.colors.field.primary,
            shape = RoundedCornerShape(TangemTheme.dimens.radius6),
        ),
    )
}

@Composable
private fun Image(@DrawableRes id: Int?, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = id != null, modifier = modifier) {
        Image(
            painter = painterResource(id = requireNotNull(id)),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
        )
    }
}

// region Preview

@Preview
@Composable
private fun Preview_WalletCard_LightTheme(
    @PreviewParameter(WalletCardStateProvider::class)
    state: WalletCardState,
) {
    TangemTheme(isDark = false) {
        WalletCard(state = state)
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