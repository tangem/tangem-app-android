package com.tangem.core.ui.components.inputrow

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.R
import com.tangem.core.ui.components.atoms.text.EllipsisText
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

/**
 * Input row component with selector
 * [Input Row Image](https://www.figma.com/design/14ISV23YB1yVW1uNVwqrKv/Android?node-id=2841-1589&t=u6pOF6lsdpvWLELb-4)
 *
 * @param title title text
 * @param subtitle subtitle text
 * @param caption caption text
 * @param infoTitle info title text
 * @param infoSubtitle info subtitle text
 * @param modifier modifier
 * @param imageUrl icon to load
 * @param subtitleColor subtitle text color
 * @param captionColor caption text color
 * @param isGrayscaleImage whether to display grayscale image
 * @param iconEndRes icon to end of row
 */
@Suppress("LongParameterList")
@Composable
fun InputRowImageInfo(
    subtitle: TextReference,
    infoTitle: TextReference,
    imageUrl: String,
    modifier: Modifier = Modifier,
    title: TextReference? = null,
    caption: TextReference? = null,
    infoSubtitle: TextReference? = null,
    subtitleColor: Color = TangemTheme.colors.text.primary1,
    captionColor: Color = TangemTheme.colors.text.tertiary,
    isGrayscaleImage: Boolean = false,
    iconEndRes: Int? = null,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing6),
        modifier = modifier
            .padding(TangemTheme.dimens.spacing12),
    ) {
        if (title != null) {
            Text(
                text = title.resolveReference(),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
            )
        }
        InputRowImageBase(
            subtitle = subtitle,
            caption = caption,
            imageUrl = imageUrl,
            subtitleColor = subtitleColor,
            captionColor = captionColor,
            isGrayscaleImage = isGrayscaleImage,
            iconEndRes = iconEndRes,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing2),
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(1f),
            ) {
                if (infoTitle is TextReference.Annotated) {
                    Text(
                        text = infoTitle.resolveAnnotatedReference(),
                        style = TangemTheme.typography.body2,
                        color = TangemTheme.colors.text.primary1,
                    )
                } else {
                    EllipsisText(
                        text = infoTitle.resolveReference(),
                        style = TangemTheme.typography.body2,
                        color = TangemTheme.colors.text.primary1,
                    )
                }
                infoSubtitle?.let {
                    if (infoSubtitle is TextReference.Annotated) {
                        Text(
                            text = infoSubtitle.resolveAnnotatedReference(),
                            style = TangemTheme.typography.caption2,
                            color = TangemTheme.colors.text.tertiary,
                        )
                    } else {
                        EllipsisText(
                            text = infoSubtitle.resolveReference(),
                            style = TangemTheme.typography.caption2,
                            color = TangemTheme.colors.text.tertiary,
                        )
                    }
                }
            }
        }
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun InputRowImageInfo_Preview(
    @PreviewParameter(InputRowImageInfoPreviewDataProvider::class) data: InputRowImageInfoPreviewData,
) {
    TangemThemePreview {
        InputRowImageInfo(
            title = data.title,
            subtitle = data.subtitle,
            caption = data.caption,
            infoTitle = data.infoTitle,
            infoSubtitle = data.infoSubtitle,
            imageUrl = "",
            iconEndRes = R.drawable.ic_chevron_right_24,
        )
    }
}

private class InputRowImageInfoPreviewDataProvider : PreviewParameterProvider<InputRowImageInfoPreviewData> {
    override val values: Sequence<InputRowImageInfoPreviewData>
        get() = sequenceOf(
            InputRowImageInfoPreviewData(
                title = stringReference("Validator"),
                subtitle = stringReference("Binance"),
                caption = combinedReference(
                    resourceReference(R.string.staking_details_apr),
                    annotatedReference(
                        buildAnnotatedString {
                            append(" ")
                            append("3,54%")
                        },
                    ),
                ),
                infoTitle = stringReference("5431231231231231231231232 USD"),
                infoSubtitle = stringReference("5 SOL"),
            ),
            InputRowImageInfoPreviewData(
                title = null,
                subtitle = stringReference("Binance"),
                caption = null,
                infoTitle = stringReference("5431231231231231231231232 USD"),
                infoSubtitle = null,
            ),
        )
}

private data class InputRowImageInfoPreviewData(
    val title: TextReference?,
    val subtitle: TextReference,
    val caption: TextReference?,
    val infoTitle: TextReference,
    val infoSubtitle: TextReference?,
)
// endregion