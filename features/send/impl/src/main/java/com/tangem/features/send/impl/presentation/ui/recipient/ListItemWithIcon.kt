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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
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
 * @param subtitleIconRes icon
 */
@Composable
fun ListItemWithIcon(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes subtitleIconRes: Int? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(TangemTheme.colors.background.action)
            .clickable { onClick() }
            .padding(
                vertical = TangemTheme.dimens.spacing8,
                horizontal = TangemTheme.dimens.spacing12,
            ),
    ) {
        IdentIcon(
            address = title,
            modifier = Modifier
                .size(TangemTheme.dimens.size40)
                .clip(RoundedCornerShape(TangemTheme.dimens.radius20)),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = TangemTheme.dimens.spacing12,
                    top = TangemTheme.dimens.spacing2,
                    bottom = TangemTheme.dimens.spacing2,
                ),
        ) {
            MiddleEllipsisText(
                text = title,
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Justify,
                modifier = Modifier.fillMaxSize(),
            )
            Row {
                subtitleIconRes?.let { iconRes ->
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = TangemTheme.colors.icon.informative,
                        modifier = Modifier
                            .size(TangemTheme.dimens.size16)
                            .background(TangemTheme.colors.background.tertiary, CircleShape)
                            .padding(TangemTheme.dimens.spacing3),
                    )
                }
                Text(
                    text = subtitle,
                    style = TangemTheme.typography.caption2,
                    color = TangemTheme.colors.text.tertiary,
                    maxLines = 1,
                    modifier = Modifier
                        .then(
                            if (subtitleIconRes != null) {
                                Modifier.padding(start = TangemTheme.dimens.spacing4)
                            } else {
                                Modifier
                            },
                        ),
                )
            }
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
            subtitleIconRes = config.iconRes,
            onClick = {},
        )
    }
}

private data class ListItemWithIconPreviewConfig(
    val title: String,
    val subtitle: String,
    val iconRes: Int? = null,
)

private class ListItemWithIconPreviewProvider : CollectionPreviewParameterProvider<ListItemWithIconPreviewConfig>(
    collection = listOf(
        ListItemWithIconPreviewConfig(
            title = "0x34B4492A412D84A6E606288f3Bd714b89135D4dE",
            subtitle = "Wallet",
            iconRes = R.drawable.ic_arrow_down_24,
        ),
        ListItemWithIconPreviewConfig(
            title = "0x34B4492A412D84A6E606288f3Bd714b89135D4dE",
            subtitle = "Wallet",
        ),
    ),
)
//endregion