package com.tangem.core.ui.components.notifications

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH8
import com.tangem.core.ui.components.buttons.common.TangemButton
import com.tangem.core.ui.components.buttons.common.TangemButtonColors
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemColorPalette.White
import com.tangem.core.ui.res.TangemTheme

/**
 * Travala notification with image background
 * @see <a href="https://www.figma.com/file/Vs6SkVsFnUPsSCNwlnVf5U/Android-%E2%80%93-UI?type=design&node-id=11690-12057&mode=design&t=eFnsA9sNytcQIoQ4-4">Travala Promo</a>
 */
@Suppress("LongMethod", "DestructuringDeclarationWithTooManyEntries")
@Composable
fun TravalaNotificationWithBackground(config: NotificationConfig, modifier: Modifier = Modifier) {
    val button = config.buttonsState as? NotificationConfig.ButtonsState.SecondaryButtonConfig

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = TangemTheme.dimens.size62)
            .background(Color.Black)
            .fillMaxWidth()
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .clickable(
                enabled = config.onClick != null,
                onClick = config.onClick ?: {},
            ),
        propagateMinConstraints = true,
        contentAlignment = Alignment.TopStart,
    ) {
        val density = LocalDensity.current
        Image(
            painter = painterResource(R.drawable.img_travala_banner_promo_background),
            contentDescription = null,
            contentScale = TravalaBackgroundScale(density),
            alignment = Alignment.TopStart,
            modifier = Modifier
                .matchParentSize()
                .wrapContentSize(unbounded = true, align = Alignment.TopStart)
                .align(Alignment.TopStart),
        )
        Image(
            painter = painterResource(R.drawable.img_travala_banner_promo_background_2),
            contentDescription = null,
            contentScale = TravalaBackgroundScale(density),
            alignment = Alignment.TopStart,
            modifier = Modifier
                .matchParentSize()
                .wrapContentSize(unbounded = true, align = Alignment.TopEnd)
                .align(Alignment.TopEnd),
        )
        Column {
            Row {
                Box(modifier = Modifier.size(87.dp))
                Column(
                    Modifier
                        .weight(1f)
                        .padding(top = TangemTheme.dimens.spacing12),
                ) {
                    Text(
                        text = config.title.resolveReference(),
                        style = TangemTheme.typography.button.copy(
                            lineBreak = LineBreak.Heading,
                        ),
                        color = TangemTheme.colors.text.constantWhite,
                    )
                    SpacerH8()
                    Text(
                        text = formatSubtitle(config.subtitle.resolveReference()),
                        style = TangemTheme.typography.caption2.copy(
                            lineBreak = LineBreak.Heading,
                        ),
                        color = TangemTheme.colors.text.constantWhite,
                    )
                }
                Icon(
                    painter = painterResource(id = R.drawable.ic_close_24),
                    contentDescription = null,
                    tint = TangemTheme.colors.text.constantWhite,
                    modifier = Modifier
                        .padding(
                            top = TangemTheme.dimens.spacing12,
                            end = TangemTheme.dimens.spacing12,
                            start = TangemTheme.dimens.spacing2,
                        )
                        .size(TangemTheme.dimens.size16)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(bounded = false),
                        ) {
                            config.onCloseClick?.invoke()
                        },
                )
            }

            TangemButton(
                text = button?.text?.resolveReference().orEmpty(),
                icon = TangemButtonIconPosition.None,
                onClick = button?.onClick ?: {},
                colors = TangemButtonColors(
                    backgroundColor = White.copy(alpha = 0.3f),
                    contentColor = White,
                    disabledBackgroundColor = TangemTheme.colors.button.disabled,
                    disabledContentColor = TangemTheme.colors.text.disabled,
                ),
                enabled = true,
                showProgress = false,
                modifier = Modifier
                    .padding(TangemTheme.dimens.spacing12)
                    .fillMaxWidth(),
            )
        }
    }
}

private const val TRAVALA_BACKGROUND_SRC_IMG_SCALE = 4

private class TravalaBackgroundScale(
    val density: Density,
) : ContentScale {
    override fun computeScaleFactor(srcSize: Size, dstSize: Size): ScaleFactor {
        with(density) {
            val originalWidth = (srcSize.width / TRAVALA_BACKGROUND_SRC_IMG_SCALE).dp.toPx()
            val widthScale = originalWidth / srcSize.width
            val originalHeight = (srcSize.height / TRAVALA_BACKGROUND_SRC_IMG_SCALE).dp.toPx()
            val heightScale = originalHeight / srcSize.height
            return ScaleFactor(widthScale, heightScale)
        }
    }
}

@Composable
private fun formatSubtitle(subtitle: String): AnnotatedString {
    val pattern = Regex("\\*\\*(.*?)\\*\\*")
    var startIndex = 0
    val annotatedString = buildAnnotatedString {
        pattern.findAll(subtitle).forEach { matchResult ->
            val index = matchResult.range.first
            val matchedValue = matchResult.groups[1]?.value ?: ""

            // appends unformatted part
            append(subtitle.substring(startIndex, index))

            // applies style on ^^-wrapped parts
            withStyle(SpanStyle(fontWeight = TangemTheme.typography.caption1.fontWeight)) {
                append(matchedValue)
            }

            // goes to next part
            startIndex = matchResult.range.last + 1
        }

        // appends remaining ending if exists
        append(subtitle.substring(startIndex))
    }

    return annotatedString
}

//region preview
@Preview
@Composable
private fun TravalaNotificationWithBackgroundPreview() {
    TangemTheme {
        TravalaNotificationWithBackground(
            config = NotificationConfig(
                title = resourceReference(
                    id = R.string.main_travala_promotion_title,
                ),
                subtitle = resourceReference(
                    id = R.string.main_travala_promotion_description,
                    formatArgs = wrappedList("May 13", "June 12"),
                ),
                iconResId = R.drawable.img_swap_promo,
                backgroundResId = R.drawable.img_travala_banner_promo_background,
                buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                    text = resourceReference(id = R.string.token_swap_promotion_button),
                    onClick = {},
                ),
            ),
        )
    }
}
//endregion