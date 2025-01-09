package com.tangem.core.ui.components.inputrow

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.components.inputrow.inner.DividerContainer
import com.tangem.core.ui.components.inputrow.inner.InputRowAsyncImage
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

/**
 * [Input Row Best Rate](https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?type=design&node-id=2100-889&mode=dev)
 *
 * @param imageUrl image source url
 * @param title title
 * @param titleExtra title extra
 * @param subtitle subtitle
 * @param modifier composable modifier
 * @param showTag show tag
 * @param showDivider show divider
 * @param onIconClick icon click
 */
@Composable
fun InputRowBestRate(
    imageUrl: String,
    title: TextReference,
    titleExtra: TextReference,
    subtitle: TextReference,
    modifier: Modifier = Modifier,
    showTag: Boolean = false,
    showDivider: Boolean = false,
    onIconClick: (() -> Unit)? = null,
) {
    DividerContainer(
        showDivider = showDivider,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(TangemTheme.dimens.spacing12),
        ) {
            InputRowAsyncImage(imageUrl = imageUrl, modifier = Modifier.size(TangemTheme.dimens.spacing40))
            Column(
                modifier = Modifier
                    .padding(start = TangemTheme.dimens.spacing12),
            ) {
                InnerTitle(
                    title = title,
                    titleExtra = titleExtra,
                    showTag = showTag,
                )
                Text(
                    text = subtitle.resolveReference(),
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.tertiary,
                    modifier = Modifier.padding(top = TangemTheme.dimens.spacing8),
                )
            }
            SpacerWMax()
            onIconClick?.let {
                Icon(
                    painter = painterResource(id = R.drawable.ic_chevron_right_24),
                    contentDescription = null,
                    tint = TangemTheme.colors.icon.informative,
                    modifier = Modifier
                        .padding(vertical = TangemTheme.dimens.spacing10)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false),
                            onClick = onIconClick,
                        ),
                )
            }
        }
    }
}

@Composable
private fun InnerTitle(title: TextReference, titleExtra: TextReference, showTag: Boolean = false) {
    Row {
        Text(
            text = title.resolveReference(),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.primary1,
        )
        Text(
            text = titleExtra.resolveReference(),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
            modifier = Modifier
                .padding(start = TangemTheme.dimens.spacing4),
        )
        if (showTag) {
            Text(
                text = stringResourceSafe(R.string.express_provider_best_rate),
                style = TangemTheme.typography.caption1,
                color = TangemTheme.colors.icon.accent,
                modifier = Modifier
                    .padding(start = TangemTheme.dimens.spacing4)
                    .background(
                        color = TangemTheme.colors.icon.accent.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(TangemTheme.dimens.radius20),
                    )
                    .padding(horizontal = TangemTheme.dimens.spacing6),
            )
        }
    }
}

//region preview
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun InputRowBestRatePreview(
    @PreviewParameter(InputRowBestRatePreviewDataProvider::class) data: InputRowBestRatePreviewData,
) {
    TangemThemePreview {
        InputRowBestRate(
            imageUrl = "",
            title = data.title,
            titleExtra = data.titleExtra,
            subtitle = data.subtitle,
            showTag = data.showTag,
            onIconClick = data.iconClick,
            modifier = Modifier
                .background(TangemTheme.colors.background.action),
        )
    }
}

private data class InputRowBestRatePreviewData(
    val title: TextReference,
    val titleExtra: TextReference,
    val showTag: Boolean,
    val subtitle: TextReference,
    val iconClick: (() -> Unit)?,
)

private class InputRowBestRatePreviewDataProvider : PreviewParameterProvider<InputRowBestRatePreviewData> {
    override val values: Sequence<InputRowBestRatePreviewData>
        get() = sequenceOf(
            InputRowBestRatePreviewData(
                title = TextReference.Str("1inch"),
                titleExtra = TextReference.Str("DEX"),
                subtitle = TextReference.Str("0,64554846 DAI ≈ 1 MATIC "),
                showTag = true,
                iconClick = {},
            ),
            InputRowBestRatePreviewData(
                title = TextReference.Str("ChangeNow"),
                titleExtra = TextReference.Str("CEX"),
                subtitle = TextReference.Str("0,64554846 DAI ≈ 1 MATIC "),
                showTag = false,
                iconClick = null,
            ),
        )
}
//endregion