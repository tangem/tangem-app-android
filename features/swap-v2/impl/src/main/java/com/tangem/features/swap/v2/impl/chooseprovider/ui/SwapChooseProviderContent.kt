package com.tangem.features.swap.v2.impl.chooseprovider.ui

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstrainedLayoutReference
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintLayoutScope
import androidx.constraintlayout.compose.Visibility
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SpacerW8
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.domain.express.models.ExpressRateType
import com.tangem.features.swap.v2.impl.R
import kotlinx.coroutines.delay

@Suppress("LongParameterList")
@Composable
fun SwapChooseProviderContent(
    expressProvider: ExpressProvider?,
    isBestRate: Boolean,
    showBestRateAnimation: Boolean,
    onClick: () -> Unit,
    onFinishAnimation: () -> Unit,
    showFCAWarning: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(),
            onClick = onClick,
        ),
    ) {
        HorizontalDivider(
            thickness = 0.5.dp,
            color = TangemTheme.colors.stroke.primary,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = rememberVectorPainter(
                    ImageVector.vectorResource(R.drawable.ic_stack_new_24),
                ),
                tint = TangemTheme.colors.icon.accent,
                contentDescription = null,
                modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp),
            )
            Text(
                text = stringResourceSafe(R.string.express_provider),
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.primary1,
                modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 12.dp),
            )
            SpacerWMax()
            ProviderInfo(expressProvider, isBestRate, showBestRateAnimation, onFinishAnimation)
        }
        if (showFCAWarning) {
            FcaProviderWarning(
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp),
            )
        }
    }
}

@Composable
private fun FcaProviderWarning(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(TangemTheme.dimens.size16),
            painter = painterResource(id = R.drawable.ic_warning_16),
            tint = TangemTheme.colors.icon.informative,
            contentDescription = null,
        )
        SpacerW8()
        Text(
            text = stringResourceSafe(R.string.express_provider_in_fca_warning_list),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
        )
    }
}

@Composable
private fun ProviderInfo(
    expressProvider: ExpressProvider?,
    isBestRate: Boolean,
    showBestRateAnimation: Boolean,
    onFinishAnimation: () -> Unit,
) {
    ConstraintLayout {
        val (imageRef, nameRef, iconRef) = createRefs()
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context = LocalContext.current)
                .data(expressProvider?.imageLarge)
                .crossfade(enable = true)
                .allowHardware(false)
                .build(),
            loading = { RectangleShimmer(radius = 4.dp) },
            error = {
                Box(
                    modifier = Modifier.background(
                        color = TangemColorPalette.Light1,
                        shape = RoundedCornerShape(4.dp),
                    ),
                )
            },
            contentDescription = null,
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .constrainAs(imageRef) {
                    start.linkTo(parent.start)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                },
        )
        Text(
            text = expressProvider?.name.orEmpty(),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.tertiary,
            modifier = Modifier
                .padding(start = 6.dp)
                .constrainAs(nameRef) {
                    start.linkTo(imageRef.end)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                },
        )
        Icon(
            painter = rememberVectorPainter(
                ImageVector.vectorResource(R.drawable.ic_chevron_24),
            ),
            tint = TangemTheme.colors.icon.informative,
            contentDescription = null,
            modifier = Modifier
                .padding(start = 4.dp)
                .constrainAs(iconRef) {
                    start.linkTo(nameRef.end)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    end.linkTo(parent.end, 12.dp)
                },
        )
        BestRateBadge(
            showBestRateAnimation = showBestRateAnimation,
            isBestRate = isBestRate,
            ref = imageRef,
            onFinishAnimation = onFinishAnimation,
        )
    }
}

@Suppress("MagicNumber", "LongMethod")
@Composable
private fun ConstraintLayoutScope.BestRateBadge(
    showBestRateAnimation: Boolean,
    isBestRate: Boolean,
    ref: ConstrainedLayoutReference,
    onFinishAnimation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val animateState = remember { MutableTransitionState(false) }

    LaunchedEffect(showBestRateAnimation) {
        if (showBestRateAnimation) {
            delay(600L)
            animateState.targetState = true
            delay(1_500L)
            animateState.targetState = false
            onFinishAnimation()
        }
    }

    val iconSize by animateDpAsState(
        label = "iconSize",
        targetValue = if (animateState.targetState) {
            12.dp
        } else {
            8.dp
        },
    )
    val iconVerticalPaddings by animateDpAsState(
        label = "iconVerticalPaddings",
        targetValue = if (animateState.targetState) {
            3.dp
        } else {
            2.dp
        },
    )
    val iconHorizontalPaddings by animateDpAsState(
        label = "iconHorizontalPaddings",
        targetValue = if (animateState.targetState) {
            4.dp
        } else {
            2.dp
        },
    )

    val startMargin by animateDpAsState(
        label = "startMargin",
        targetValue = if (animateState.targetState) {
            (-12).dp
        } else {
            (-10).dp
        },
    )

    val topMargin by animateDpAsState(
        label = "topMargin",
        targetValue = if (animateState.targetState) {
            (-12).dp
        } else {
            (-9).dp
        },
    )

    Row(
        modifier = modifier
            .constrainAs(createRef()) {
                start.linkTo(ref.end, startMargin)
                top.linkTo(ref.bottom, topMargin)
                visibility = if (isBestRate) Visibility.Visible else Visibility.Gone
            }
            .background(TangemTheme.colors.stroke.transparency, RoundedCornerShape(120.dp))
            .padding(1.5.dp)
            .background(TangemTheme.colors.icon.accent, RoundedCornerShape(120.dp)),
    ) {
        Icon(
            painter = rememberVectorPainter(
                ImageVector.vectorResource(R.drawable.ic_rounded_star_24),
            ),
            contentDescription = null,
            tint = TangemTheme.colors.icon.constant,
            modifier = Modifier
                .padding(iconHorizontalPaddings, iconVerticalPaddings)
                .size(iconSize),
        )
        AnimatedVisibility(
            visibleState = animateState,
            enter = expandIn() + fadeIn(),
            exit = shrinkOut() + fadeOut(),
            label = "textAnimation",
            modifier = Modifier.padding(end = 6.dp),
        ) {
            Text(
                text = stringResourceSafe(R.string.express_provider_best_rate),
                style = TangemTheme.typography.caption1,
                color = TangemTheme.colors.text.constantWhite,
            )
        }
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun SwapChooseProviderContent_Preview() {
    TangemThemePreview {
        Box(
            modifier = Modifier.background(TangemTheme.colors.background.tertiary),
        ) {
            SwapChooseProviderContent(
                isBestRate = true,
                showBestRateAnimation = true,
                expressProvider = ExpressProvider(
                    providerId = "changelly",
                    rateTypes = listOf(ExpressRateType.Fixed),
                    name = "Changelly",
                    type = ExpressProviderType.CEX,
                    imageLarge = "",
                    termsOfUse = "",
                    privacyPolicy = "",
                    isRecommended = false,
                    slippage = null,
                ),
                onClick = {},
                showFCAWarning = true,
                onFinishAnimation = {},
            )
        }
    }
}
// endregion