package com.tangem.features.send.impl.presentation.ui.recipient

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.CircleShimmer
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.atoms.text.EllipsisText
import com.tangem.core.ui.components.atoms.text.TextEllipsis
import com.tangem.core.ui.components.icons.identicon.IdentIcon
import com.tangem.core.ui.extensions.rememberHapticFeedback
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.impl.R

/**
 * Row item with title and subtitle
 *
 * @param title title
 * @param subtitle subtitle
 * @param onClick click listener
 * @param modifier modifier
 * @param info info
 * @param subtitleEndOffset offset for subtitle ellipsis
 * @param subtitleIconRes icon
 */
@Composable
fun ListItemWithIcon(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    info: String? = null,
    subtitleEndOffset: Int = 0,
    @DrawableRes subtitleIconRes: Int? = null,
    isLoading: Boolean = false,
) {
    AnimatedContent(
        targetState = isLoading,
        label = "Recent List Content Animation",
        transitionSpec = { fadeIn().togetherWith(fadeOut()) },
    ) { isLoadingState ->
        if (isLoadingState) {
            ListItemLoading(modifier = modifier)
        } else {
            ListItemWithIcon(
                title = title,
                subtitle = subtitle,
                onClick = onClick,
                info = info,
                subtitleEndOffset = subtitleEndOffset,
                subtitleIconRes = subtitleIconRes,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun ListItemWithIcon(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    info: String? = null,
    subtitleEndOffset: Int = 0,
    @DrawableRes subtitleIconRes: Int? = null,
) {
    val hapticFeedback = rememberHapticFeedback(state = title, onAction = onClick)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable { hapticFeedback() }
            .padding(horizontal = TangemTheme.dimens.spacing12),
    ) {
        IdentIcon(
            address = title,
            modifier = Modifier
                .padding(vertical = TangemTheme.dimens.spacing8)
                .size(TangemTheme.dimens.size40)
                .clip(RoundedCornerShape(TangemTheme.dimens.radius20)),
        )
        Column(
            modifier = Modifier
                .height(TangemTheme.dimens.size36)
                .padding(start = TangemTheme.dimens.spacing12),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            EllipsisText(
                text = title,
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Justify,
                ellipsis = TextEllipsis.Middle,
                modifier = Modifier,
            )
            Row {
                if (subtitleIconRes != null) {
                    Icon(
                        painter = painterResource(id = subtitleIconRes),
                        contentDescription = null,
                        tint = TangemTheme.colors.icon.informative,
                        modifier = Modifier
                            .padding(end = TangemTheme.dimens.spacing2)
                            .size(TangemTheme.dimens.size16)
                            .background(TangemTheme.colors.background.tertiary, CircleShape)
                            .padding(TangemTheme.dimens.spacing2),
                    )
                }
                val (text, offset) = remember(subtitle, info) {
                    if (info != null) {
                        val suffix = ", $info"
                        subtitle + suffix to suffix.length + subtitleEndOffset
                    } else {
                        subtitle to 0
                    }
                }
                EllipsisText(
                    text = text,
                    style = TangemTheme.typography.caption2,
                    color = TangemTheme.colors.text.tertiary,
                    ellipsis = TextEllipsis.OffsetEnd(offsetEnd = offset),
                )
            }
        }
    }
}

@Composable
private fun ListItemLoading(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = TangemTheme.dimens.spacing12),
    ) {
        CircleShimmer(
            modifier = Modifier
                .padding(vertical = TangemTheme.dimens.spacing8)
                .size(TangemTheme.dimens.size40),
        )
        Column(
            modifier = Modifier
                .height(TangemTheme.dimens.size36)
                .padding(start = TangemTheme.dimens.spacing12),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            RectangleShimmer(
                radius = TangemTheme.dimens.radius3,
                modifier = Modifier.size(
                    width = TangemTheme.dimens.spacing70,
                    height = TangemTheme.dimens.spacing12,
                ),
            )
            RectangleShimmer(
                radius = TangemTheme.dimens.radius3,
                modifier = Modifier.size(
                    width = TangemTheme.dimens.spacing52,
                    height = TangemTheme.dimens.spacing12,
                ),
            )
        }
    }
}

// region preview
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ListItemWithIconPreview(
    @PreviewParameter(ListItemWithIconPreviewProvider::class) config: ListItemWithIconPreviewConfig,
) {
    TangemThemePreview {
        ListItemWithIcon(
            title = config.title,
            subtitle = config.subtitle,
            subtitleEndOffset = config.subtitleEndOffset,
            subtitleIconRes = config.iconRes,
            onClick = {},
            isLoading = config.isLoading,
        )
    }
}

private data class ListItemWithIconPreviewConfig(
    val title: String,
    val subtitle: String,
    val info: String? = null,
    val subtitleEndOffset: Int = 0,
    val iconRes: Int? = null,
    val isLoading: Boolean = false,
)

private class ListItemWithIconPreviewProvider : CollectionPreviewParameterProvider<ListItemWithIconPreviewConfig>(
    collection = listOf(
        ListItemWithIconPreviewConfig(
            title = "0x34B4492A412D84A6E606288f3Bd714b89135D4dE",
            subtitle = "0.000000000000000000000000000000 BTC",
            info = "0.0.0000 at 00:00",
            subtitleEndOffset = "BTC".length,
            iconRes = R.drawable.ic_arrow_down_24,
        ),
        ListItemWithIconPreviewConfig(
            title = "0x34B4492A412D84A6E606288f3Bd714b89135D4dE",
            subtitle = "1 BTC",
            info = "0.0.0000 at 00:00",
            subtitleEndOffset = "BTC".length,
            iconRes = R.drawable.ic_arrow_down_24,
        ),
        ListItemWithIconPreviewConfig(
            title = "0x34B4492A412D84A6E606288f3Bd714b89135D4dE",
            subtitle = "Wallet",
        ),
        ListItemWithIconPreviewConfig(
            title = "0x34B4492A412D84A6E606288f3Bd714b89135D4dE",
            subtitle = "0.000000000000000000000000000000 BTC",
            info = "0.0.0000 at 00:00",
            subtitleEndOffset = "BTC".length,
            iconRes = R.drawable.ic_arrow_down_24,
            isLoading = true,
        ),
    ),
)
//endregion