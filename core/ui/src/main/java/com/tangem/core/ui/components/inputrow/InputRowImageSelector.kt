package com.tangem.core.ui.components.inputrow

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.components.atoms.radiobutton.TangemRadioButton
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.annotatedReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.BigDecimalFormatter
import java.math.BigDecimal

/**
 * Input row component with selector
 * [Input Row Image](https://www.figma.com/design/14ISV23YB1yVW1uNVwqrKv/Android?node-id=2772-905&t=FH84ljLBk1vmGAei-4)
 *
 * @param subtitle subtitle text
 * @param caption caption text
 * @param imageUrl icon to load
 * @param onSelect callback when selected
 * @param modifier modifier
 * @param subtitleColor subtitle text color
 * @param captionColor caption text color
 * @param isSelected true if selected
 */
@Composable
fun InputRowImageSelector(
    subtitle: TextReference,
    caption: TextReference,
    imageUrl: String,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    subtitleColor: Color = TangemTheme.colors.text.primary1,
    captionColor: Color = TangemTheme.colors.text.tertiary,
    isSelected: Boolean = false,
) {
    InputRowImageBase(
        subtitle = subtitle,
        caption = caption,
        imageUrl = imageUrl,
        subtitleColor = subtitleColor,
        captionColor = captionColor,
        modifier = modifier
            .clickable(
                onClick = onSelect,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(),
            )
            .padding(TangemTheme.dimens.spacing12),
    ) {
        SpacerWMax()
        TangemRadioButton(isSelected = isSelected, isEnabled = false, onClick = onSelect)
    }
}

//region preview
@Preview(widthDp = 328)
@Preview(widthDp = 328, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun InputRowImageSelectorPreview(
    @PreviewParameter(InputRowImageSelectorPreviewDataProvider::class) data: InputRowImageSelectorPreviewData,
) {
    TangemThemePreview {
        InputRowImageSelector(
            modifier = Modifier.background(TangemTheme.colors.background.action),
            subtitle = data.subtitle,
            caption = combinedReference(
                resourceReference(R.string.staking_details_apr),
                annotatedReference(
                    buildAnnotatedString {
                        append(" ")
                        withStyle(style = SpanStyle(color = TangemTheme.colors.text.accent)) {
                            append(
                                BigDecimalFormatter.formatPercent(BigDecimal.ZERO, true),
                            )
                        }
                    },
                ),
            ),
            imageUrl = "",
            isSelected = false,
            onSelect = {},
        )
    }
}

private data class InputRowImageSelectorPreviewData(
    val subtitle: TextReference,
    val caption: TextReference,
    val showDivider: Boolean,
    val actionIconRes: Int?,
    val isSelected: Boolean,
)

private class InputRowImageSelectorPreviewDataProvider :
    PreviewParameterProvider<InputRowImageSelectorPreviewData> {
    override val values: Sequence<InputRowImageSelectorPreviewData>
        get() = sequenceOf(
            InputRowImageSelectorPreviewData(
                subtitle = TextReference.Str("subtitle"),
                caption = TextReference.Str("caption"),
                actionIconRes = null,
                showDivider = false,
                isSelected = false,
            ),
            InputRowImageSelectorPreviewData(
                subtitle = TextReference.Str("subtitle"),
                caption = TextReference.Str("caption"),
                actionIconRes = R.drawable.ic_chevron_right_24,
                showDivider = true,
                isSelected = true,
            ),
        )
}
//endregion