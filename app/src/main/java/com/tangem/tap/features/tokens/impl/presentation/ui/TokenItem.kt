package com.tangem.tap.features.tokens.impl.presentation.ui

import android.graphics.drawable.Drawable
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.components.CurrencyPlaceholderIcon
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.common.compose.extensions.toPx
import com.tangem.tap.features.tokens.impl.presentation.states.TokenItemState
import com.tangem.wallet.R

/**
[REDACTED_AUTHOR]
 */
@Suppress("LongMethod")
@Composable
internal fun TokenItem(model: TokenItemState) {
    var isExpanded by rememberSaveable { mutableStateOf(value = false) }
    var iconBackgroundColor by remember { mutableStateOf(Color.Transparent) }

    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = TangemTheme.colors.background.secondary)
            .padding(top = TangemTheme.dimens.spacing16),
    ) {
        val (icon, title, availableNetworksText) = createRefs()
        val (briefNetworksList, detailedNetworksList, changeNetworksViewButton) = createRefs()

        val spacing16 = TangemTheme.dimens.spacing16
        val spacing6 = TangemTheme.dimens.spacing6

        Box(
            modifier = Modifier
                .background(
                    color = iconBackgroundColor,
                    shape = TangemTheme.shapes.roundedCorners8,
                )
                .size(TangemTheme.dimens.size46)
                .constrainAs(icon) {
                    top.linkTo(parent.top)
                    start.linkTo(anchor = parent.start, margin = spacing16)
                },
        ) {
            Icon(
                name = model.fullName,
                iconUrl = model.iconUrl,
                onContrastCalculate = { iconBackgroundColor = it },
            )
        }

        Title(
            title = model.fullName,
            modifier = Modifier.constrainAs(title) {
                top.linkTo(parent.top)
                start.linkTo(icon.end, margin = spacing16)
                end.linkTo(anchor = changeNetworksViewButton.start, margin = spacing16)
                width = Dimension.fillToConstraints
            },
        )

        Subtitle(
            isExpanded = isExpanded,
            modifier = Modifier.constrainAs(availableNetworksText) {
                top.linkTo(title.bottom, margin = spacing6)
                start.linkTo(icon.end, margin = spacing16)
                end.linkTo(anchor = changeNetworksViewButton.start, margin = spacing16)
                width = Dimension.fillToConstraints
            },
        )

        BriefNetworksList(
            isCollapsed = !isExpanded,
            networks = model.networks,
            modifier = Modifier.constrainAs(briefNetworksList) {
                top.linkTo(title.bottom, margin = spacing6)
                start.linkTo(icon.end, margin = spacing16)
                end.linkTo(anchor = changeNetworksViewButton.start, margin = spacing16)
                width = Dimension.fillToConstraints
            },
        )

        DetailedNetworksList(
            isExpanded = isExpanded,
            token = model,
            networks = model.networks,
            modifier = Modifier.constrainAs(detailedNetworksList) {
                top.linkTo(anchor = availableNetworksText.bottom, margin = spacing16)
                centerHorizontallyTo(parent)
                width = Dimension.fillToConstraints
            },
        )

        ChangeNetworksViewButton(
            isExpanded = isExpanded,
            onClick = { isExpanded = !isExpanded },
            modifier = Modifier.constrainAs(changeNetworksViewButton) {
                top.linkTo(parent.top)
                end.linkTo(anchor = parent.end, margin = spacing6)
                if (!isExpanded) bottom.linkTo(parent.bottom)
            },
        )
    }
}

@Composable
private fun Icon(name: String, iconUrl: String, onContrastCalculate: (Color) -> Unit, modifier: Modifier = Modifier) {
    val iconModifier = modifier.size(size = TangemTheme.dimens.size46)
    val screenBackgroundColor = TangemTheme.colors.background.secondary.toArgb()
    val isDarkTheme = isSystemInDarkTheme()

    SubcomposeAsyncImage(
        modifier = iconModifier,
        model = ImageRequest.Builder(context = LocalContext.current)
            .size(size = TangemTheme.dimens.size46.toPx().toInt())
            .data(data = iconUrl)
            .crossfade(enable = true)
            .allowHardware(false)
            .listener(
                onSuccess = { _, result ->
                    if (isDarkTheme) {
                        setContrastBackgroundIfNeeded(
                            drawable = result.drawable,
                            screenBackgroundColor = screenBackgroundColor,
                            onContrastCalculate = onContrastCalculate,
                        )
                    }
                },
            )
            .build(),
        contentDescription = null,
        loading = { CurrencyPlaceholderIcon(id = name, modifier = iconModifier) },
        error = { CurrencyPlaceholderIcon(id = name, modifier = iconModifier) },
    )
}

private fun setContrastBackgroundIfNeeded(
    drawable: Drawable,
    screenBackgroundColor: Int,
    onContrastCalculate: (Color) -> Unit,
) {
    Palette.Builder(drawable.toBitmap()).generate { palette ->
        val color = palette?.getDominantColor(screenBackgroundColor) ?: screenBackgroundColor
        val contrast = ColorUtils.calculateContrast(color, screenBackgroundColor)
        val colorToSet = if (contrast > LOW_CONTRAST_RATIO) {
            Color.Transparent
        } else {
            Color.White
        }
        onContrastCalculate(colorToSet)
    }
}

// https://www.w3.org/TR/2008/REC-WCAG20-20081211/#contrast-ratiodef
private const val LOW_CONTRAST_RATIO = 1.5f

@Composable
private fun Title(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        modifier = modifier,
        color = TangemTheme.colors.text.primary1,
        textAlign = TextAlign.Start,
        maxLines = 1,
        style = TangemTheme.typography.body1,
    )
}

@Composable
private fun Subtitle(isExpanded: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = isExpanded,
        modifier = modifier,
        enter = fadeIn() + expandVertically(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Text(
            text = stringResource(id = R.string.currency_subtitle_expanded),
            color = TangemTheme.colors.text.secondary,
            maxLines = 1,
            style = TangemTheme.typography.body2,
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ChangeNetworksViewButton(isExpanded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier.size(size = TangemTheme.dimens.size46)) {
        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = { fadeIn() + scaleIn() with scaleOut() + fadeOut() },
        ) { isExpanded ->
            Icon(
                painter = painterResource(
                    id = if (isExpanded) R.drawable.ic_arrow_extended else R.drawable.ic_arrow_collapsed,
                ),
                contentDescription = null,
                tint = TangemTheme.colors.icon.secondary,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_TokenItem_ManageAccess_Light() {
    TangemTheme(isDark = false) {
        TokenItem(model = TokenListPreviewData.createManageToken())
    }
}

@Preview()
@Composable
private fun Preview_TokenItem_ManageAccess_Dark() {
    TangemTheme(isDark = true) {
        TokenItem(model = TokenListPreviewData.createManageToken())
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_TokenItem_ReadAccess_Light() {
    TangemTheme(isDark = false) {
        TokenItem(model = TokenListPreviewData.createReadToken())
    }
}

@Preview()
@Composable
private fun Preview_TokenItem_ReadAccess_Dark() {
    TangemTheme(isDark = true) {
        TokenItem(model = TokenListPreviewData.createReadToken())
    }
}