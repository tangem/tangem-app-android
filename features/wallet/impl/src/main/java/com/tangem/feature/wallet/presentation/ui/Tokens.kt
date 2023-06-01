package com.tangem.feature.wallet.presentation.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.components.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemTypography
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.state.*

private const val DOTS = "•••"

@Composable
internal fun TokenItemV2(config: TokenV2Config) {
    when (config.tokenUIState) {
        TokenUIState.LOADED -> LoadedTokenState(config)
        TokenUIState.LOADING -> LoadingTokenState()
    }
}

@Composable
private fun LoadedTokenState(config: TokenV2Config) {
    BaseSurface {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = TangemTheme.dimens.spacing12,
                    vertical = TangemTheme.dimens.spacing4,
                ),
        ) {
            val (iconItem, tokenNameItem, fiatItem) = createRefs()
            TokenIcon(
                modifier = Modifier.constrainAs(iconItem) {
                    centerVerticallyTo(parent)
                    start.linkTo(parent.start)
                },
                tokenIconUrl = config.iconUrl,
                icon = config.icon,
                networkIconRes = config.networkIcon,
            )
            TokenTitleAmountBlock(
                modifier = Modifier
                    .padding(horizontal = TangemTheme.dimens.spacing12)
                    .constrainAs(tokenNameItem) {
                        centerVerticallyTo(parent)
                        start.linkTo(iconItem.end)
                        end.linkTo(fiatItem.start)
                        width = Dimension.fillToConstraints
                    },
                title = config.name,
                amount = config.amount,
                hasPending = config.hasPending,
            )
            TokenOptionsBlock(
                config = config,
                modifier = Modifier.constrainAs(fiatItem) {
                    centerVerticallyTo(parent)
                    end.linkTo(parent.end)
                },
            )
        }
    }
}

/**
 * Block for end part of token item
 * shows status is reachable, is drag, hidden or show balance
 */
@Composable
private fun TokenOptionsBlock(config: TokenV2Config, modifier: Modifier = Modifier) {
    when (config.tokenOptionsUIState) {
        TokenOptionsUIState.VISIBLE -> TokenFiatPercentageBlock(
            modifier = modifier,
            fiatAmount = config.fiatAmount,
            priceChange = config.priceChange,
        )
        TokenOptionsUIState.UNREACHABLE -> Text(
            modifier = modifier,
            text = "Unreachable", // TODO (conform this text)
            style = TangemTypography.body2,
            color = TangemTheme.colors.text.tertiary,
        )
        TokenOptionsUIState.HIDDEN -> TokenFiatPercentageBlock(
            modifier = modifier,
            fiatAmount = DOTS,
            priceChange = config.priceChange,
        )
        TokenOptionsUIState.DRAG -> Icon(
            modifier = modifier,
            painter = painterResource(id = R.drawable.ic_drag_24),
            tint = TangemTheme.colors.icon.informative,
            contentDescription = null,
        )
    }
}

@Composable
private fun LoadingTokenState() {
    BaseSurface {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = TangemTheme.dimens.spacing12,
                    vertical = TangemTheme.dimens.spacing4,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CircleShimmer(modifier = Modifier.size(size = TangemTheme.dimens.size42))
            SpacerW12()
            Column(verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8)) {
                ShimmerRectangle(
                    modifier = Modifier.size(
                        width = TangemTheme.dimens.size72,
                        height = TangemTheme.dimens.size12,
                    ),
                )
                ShimmerRectangle(
                    modifier = Modifier.size(
                        width = TangemTheme.dimens.size50,
                        height = TangemTheme.dimens.size12,
                    ),
                )
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
        ) {
            ShimmerRectangle(
                modifier = Modifier.size(
                    width = TangemTheme.dimens.size40,
                    height = TangemTheme.dimens.size12,
                ),
            )
            ShimmerRectangle(
                modifier = Modifier.size(
                    width = TangemTheme.dimens.size40,
                    height = TangemTheme.dimens.size12,
                ),
            )
        }
    }
}

@Composable
private fun BaseSurface(onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.defaultMinSize(minHeight = TangemTheme.dimens.size68),
        color = TangemTheme.colors.background.primary,
        onClick = onClick ?: {},
        enabled = onClick != null,
    ) {
        content()
    }
}

@Composable
private fun TokenTitleAmountBlock(title: String, amount: String, hasPending: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing2),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8)) {
            Text(
                text = title,
                style = TangemTypography.subtitle2,
                color = TangemTheme.colors.text.primary1,
            )
            if (hasPending) {
                Image(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    painter = painterResource(id = R.drawable.img_loader_15),
                    contentDescription = null,
                )
            }
        }
        Text(
            text = amount,
            style = TangemTypography.body2,
            color = TangemTheme.colors.text.tertiary,
        )
    }
}

@Composable
private fun TokenFiatPercentageBlock(fiatAmount: String, priceChange: PriceChange, modifier: Modifier = Modifier) {
    Column(modifier = modifier.requiredWidth(IntrinsicSize.Max)) {
        Text(
            modifier = Modifier.align(Alignment.End),
            text = fiatAmount,
            style = TangemTypography.body2,
            color = TangemTheme.colors.text.primary1,
        )
        SpacerH2()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            val iconChangeArrow: Int
            val changeTextColor: Color
            when (priceChange.type) {
                PriceChangeType.UP -> {
                    iconChangeArrow = R.drawable.img_arrow_up_8
                    changeTextColor = TangemTheme.colors.text.accent
                }
                PriceChangeType.DOWN -> {
                    iconChangeArrow = R.drawable.img_arrow_down_8
                    changeTextColor = TangemTheme.colors.text.warning
                }
            }
            Image(
                modifier = Modifier.align(Alignment.CenterVertically),
                painter = painterResource(id = iconChangeArrow),
                contentDescription = null,
            )
            SpacerW4()
            Text(
                modifier = Modifier.align(Alignment.CenterVertically),
                text = priceChange.valuePercent,
                style = TangemTypography.body2,
                color = changeTextColor,
            )
        }
    }
}

@Composable
private fun TokenIcon(
    tokenIconUrl: String?,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    @DrawableRes networkIconRes: Int? = null,
) {
    Box(
        modifier = modifier
            .padding(end = TangemTheme.dimens.spacing16)
            .size(TangemTheme.dimens.size42),
    ) {
        val tokenImageModifier = Modifier
            .align(Alignment.BottomStart)
            .size(TangemTheme.dimens.size36)

        val data = if (tokenIconUrl.isNullOrEmpty()) {
            icon
        } else {
            tokenIconUrl
        }
        SubcomposeAsyncImage(
            modifier = tokenImageModifier,
            model = ImageRequest.Builder(LocalContext.current)
                .data(data)
                .crossfade(true)
                .build(),
            loading = { CircleShimmer(modifier = tokenImageModifier) },
            contentDescription = null,
        )

        if (networkIconRes != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(TangemTheme.dimens.size18)
                    .background(color = Color.White, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    modifier = Modifier.padding(all = 0.5.dp),
                    painter = painterResource(id = networkIconRes),
                    contentDescription = null,
                )
            }
        }
    }
}

// region preview

@Composable
private fun TokensPreview() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing18),
    ) {
        val config = TokenV2Config(
            name = "Polygon",
            amount = "5,412 MATIC",
            fiatAmount = "321 $",
            priceChange = PriceChange(
                valuePercent = "2%",
                type = PriceChangeType.UP,
            ),
            iconUrl = null,
            icon = R.drawable.img_polygon_22,
            networkIcon = R.drawable.img_polygon_22,
            tokenUIState = TokenUIState.LOADED,
            tokenOptionsUIState = TokenOptionsUIState.VISIBLE,
            hasPending = true,
        )
        // Loaded item
        TokenItemV2(config)
        // Unreachable item
        TokenItemV2(config.copy(tokenOptionsUIState = TokenOptionsUIState.UNREACHABLE))
        // Drag item
        TokenItemV2(config.copy(tokenOptionsUIState = TokenOptionsUIState.DRAG))
        // Hidden item
        TokenItemV2(config.copy(tokenOptionsUIState = TokenOptionsUIState.UNREACHABLE))
        // Loading item
        TokenItemV2(
            config.copy(
                tokenUIState = TokenUIState.LOADING,
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_Tokens_InLightTheme() {
    TangemTheme(isDark = false) {
        TokensPreview()
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_Tokens_InDarkTheme() {
    TangemTheme(isDark = true) {
        TokensPreview()
    }
}

// endregion preview