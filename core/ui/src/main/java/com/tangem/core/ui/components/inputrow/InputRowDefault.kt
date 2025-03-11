package com.tangem.core.ui.components.inputrow

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.R
import com.tangem.core.ui.components.inputrow.inner.DividerContainer
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

/**
 * [InputRowDefault](https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?type=design&node-id=2100-807&mode=design&t=86eKp9izWxUvmoCq-4)
 *
 * @param title title reference
 * @param text primary text reference
 * @param modifier modifier
 * @param titleColor title color
 * @param textColor text color
 * @param iconRes action icon
 * @param iconTint action icon tint
 * @param onIconClick click on action icon
 * @param showDivider show divider
 * @see [InputRowEnter] for editable version
 */
@Composable
fun InputRowDefault(
    text: TextReference,
    modifier: Modifier = Modifier,
    title: TextReference? = null,
    titleColor: Color = TangemTheme.colors.text.secondary,
    textColor: Color = TangemTheme.colors.text.primary1,
    iconRes: Int? = null,
    iconTint: Color = TangemTheme.colors.icon.informative,
    onIconClick: (() -> Unit)? = null,
    showDivider: Boolean = false,
) {
    DividerContainer(
        modifier = modifier,
        showDivider = showDivider,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TangemTheme.dimens.spacing12),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f),
            ) {
                title?.let {
                    Text(
                        text = title.resolveReference(),
                        style = TangemTheme.typography.subtitle2,
                        color = titleColor,
                        modifier = Modifier.padding(bottom = TangemTheme.dimens.spacing8),
                    )
                }
                Text(
                    text = text.resolveReference(),
                    style = TangemTheme.typography.body2,
                    color = textColor,
                )
            }
            iconRes?.let {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier
                        .align(CenterVertically)
                        .padding(
                            top = TangemTheme.dimens.spacing10,
                            bottom = TangemTheme.dimens.spacing10,
                        )
                        .clickable(
                            enabled = onIconClick != null,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false),
                        ) { onIconClick?.invoke() },
                )
            }
        }
    }
}

//region preview
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun InputRowDefaultPreview(
    @PreviewParameter(InputRowDefaultPreviewDataProvider::class) data: InputRowDefaultPreviewData,
) {
    TangemThemePreview {
        InputRowDefault(
            title = TextReference.Str(data.title),
            text = TextReference.Str(data.text),
            iconRes = data.iconRes,
            showDivider = data.showDivider,
            modifier = Modifier.background(TangemTheme.colors.background.action),
        )
    }
}

private data class InputRowDefaultPreviewData(
    val title: String,
    val text: String,
    val iconRes: Int?,
    val showDivider: Boolean,
)

private class InputRowDefaultPreviewDataProvider : PreviewParameterProvider<InputRowDefaultPreviewData> {
    override val values: Sequence<InputRowDefaultPreviewData>
        get() = sequenceOf(
            InputRowDefaultPreviewData(
                title = "title",
                text = "text",
                iconRes = null,
                showDivider = true,
            ),
            InputRowDefaultPreviewData(
                title = "title",
                text = "text",
                iconRes = R.drawable.ic_chevron_right_24,
                showDivider = false,
            ),
        )
}
//endregion