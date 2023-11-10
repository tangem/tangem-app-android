package com.tangem.features.send.impl.presentation.ui.recipient

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Visibility
import com.tangem.core.ui.components.MiddleEllipsisText
import com.tangem.core.ui.components.icons.identicon.IdentIcon
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
 * @param subtitleIconRes icon
 */
@Suppress("DestructuringDeclarationWithTooManyEntries", "LongMethod")
@Composable
fun ListItemWithIcon(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    info: String? = null,
    @DrawableRes subtitleIconRes: Int? = null,
) {
    ConstraintLayout(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = TangemTheme.dimens.spacing12),
    ) {
        val (iconRef, titleRef, subtitleRef, subtitleIconRef, infoRef) = createRefs()

        val spacing2 = TangemTheme.dimens.spacing2
        val spacing8 = TangemTheme.dimens.spacing8
        val spacing10 = TangemTheme.dimens.spacing10
        val spacing12 = TangemTheme.dimens.spacing12
        IdentIcon(
            address = title,
            modifier = Modifier
                .size(TangemTheme.dimens.size40)
                .clip(RoundedCornerShape(TangemTheme.dimens.radius20))
                .constrainAs(iconRef) {
                    start.linkTo(parent.start)
                    top.linkTo(parent.top, margin = spacing8)
                    bottom.linkTo(parent.bottom, margin = spacing8)
                },
        )
        MiddleEllipsisText(
            text = title,
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Justify,
            modifier = Modifier
                .constrainAs(titleRef) {
                    start.linkTo(iconRef.end, margin = spacing12)
                    end.linkTo(parent.end)
                    top.linkTo(parent.top, margin = spacing10)
                    width = Dimension.fillToConstraints
                },
        )
        Icon(
            painter = painterResource(id = subtitleIconRes ?: R.drawable.ic_arrow_down_24),
            contentDescription = null,
            tint = TangemTheme.colors.icon.informative,
            modifier = Modifier
                .size(TangemTheme.dimens.size16)
                .background(TangemTheme.colors.background.tertiary, CircleShape)
                .constrainAs(subtitleIconRef) {
                    start.linkTo(iconRef.end, margin = spacing12)
                    top.linkTo(titleRef.bottom)
                    bottom.linkTo(parent.bottom, margin = spacing10)
                    visibility = if (subtitleIconRes == null) Visibility.Gone else Visibility.Visible
                },
        )
        Text(
            text = subtitle,
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .constrainAs(subtitleRef) {
                    start.linkTo(subtitleIconRef.end, margin = spacing2, goneMargin = spacing12)
                    end.linkTo(infoRef.start)
                    top.linkTo(titleRef.bottom)
                    bottom.linkTo(parent.bottom, margin = spacing10)
                    width = Dimension.fillToConstraints
                },
        )
        info?.let {
            Text(
                text = it,
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
                maxLines = 1,
                modifier = Modifier
                    .constrainAs(infoRef) {
                        start.linkTo(subtitleRef.end, goneMargin = spacing12)
                        end.linkTo(parent.end)
                        top.linkTo(titleRef.bottom)
                        bottom.linkTo(parent.bottom, margin = spacing10)
                    },
            )
        }
    }
}

// region preview
@Preview
@Composable
private fun ListItemWithIconPreview_Light(
    @PreviewParameter(ListItemWithIconPreviewProvider::class) config: ListItemWithIconPreviewConfig,
) {
    TangemTheme {
        ListItemWithIcon(
            title = config.title,
            subtitle = config.subtitle,
            info = config.info,
            subtitleIconRes = config.iconRes,
            onClick = {},
        )
    }
}

@Preview
@Composable
private fun ListItemWithIconPreview_Dark(
    @PreviewParameter(ListItemWithIconPreviewProvider::class) config: ListItemWithIconPreviewConfig,
) {
    TangemTheme(isDark = true) {
        ListItemWithIcon(
            title = config.title,
            subtitle = config.subtitle,
            info = config.info,
            subtitleIconRes = config.iconRes,
            onClick = {},
        )
    }
}

private data class ListItemWithIconPreviewConfig(
    val title: String,
    val subtitle: String,
    val info: String? = null,
    val iconRes: Int? = null,
)

private class ListItemWithIconPreviewProvider : CollectionPreviewParameterProvider<ListItemWithIconPreviewConfig>(
    collection = listOf(
        ListItemWithIconPreviewConfig(
            title = "0x34B4492A412D84A6E606288f3Bd714b89135D4dE",
            subtitle = "0.000000000000000000000000000000 BTC",
            info = "0.0.0000 at 00:00",
            iconRes = R.drawable.ic_arrow_down_24,
        ),
        ListItemWithIconPreviewConfig(
            title = "0x34B4492A412D84A6E606288f3Bd714b89135D4dE",
            subtitle = "1 BTC",
            info = "0.0.0000 at 00:00",
            iconRes = R.drawable.ic_arrow_down_24,
        ),
        ListItemWithIconPreviewConfig(
            title = "0x34B4492A412D84A6E606288f3Bd714b89135D4dE",
            subtitle = "Wallet",
        ),
    ),
)
//endregion