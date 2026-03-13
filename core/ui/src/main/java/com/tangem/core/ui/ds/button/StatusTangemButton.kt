package com.tangem.core.ui.ds.button

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

/**
 * [Accent Tangem button](https://www.figma.com/design/RU7AIgwHtGdMfy83T5UOoR/Core-Library?node-id=8004-26798)
 *
 * @param buttonUM     TangemButtonUM data model containing button properties.
 * @param modifier     Modifier to be applied to the button.
 */
@Composable
fun StatusTangemButton(buttonUM: TangemButtonUM, modifier: Modifier = Modifier) {
    StatusTangemButton(
        onClick = buttonUM.onClick,
        modifier = modifier,
        text = buttonUM.text,
        iconRes = buttonUM.iconRes,
        iconPosition = buttonUM.iconPosition,
        isEnabled = buttonUM.isEnabled,
        isLoading = buttonUM.isLoading,
        type = buttonUM.type,
        size = buttonUM.size,
        shape = buttonUM.shape,
    )
}

/**
 * [Accent Tangem button](https://www.figma.com/design/RU7AIgwHtGdMfy83T5UOoR/Core-Library?node-id=8004-26798)
 *
 * @param onClick       Lambda to be invoked when the button is clicked.
 * @param modifier      Modifier to be applied to the button.
 * @param text          TextReference for the button label.
 * @param iconRes       Drawable resource ID for the icon to be displayed in the button.
 * @param iconPosition  Position of the icon (Start or End).
 * @param isEnabled     Boolean indicating whether the button is enabled.
 * @param isLoading     Boolean indicating whether the button is in a loading state.
 * @param size          TangemButtonSize defining the size of the button.
 * @param shape         TangemButtonShape defining the shape of the button.
 *
[REDACTED_AUTHOR]
 */
@Composable
fun StatusTangemButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: TextReference? = null,
    @DrawableRes iconRes: Int? = null,
    iconPosition: TangemButtonIconPosition = TangemButtonIconPosition.Start,
    isEnabled: Boolean = true,
    isLoading: Boolean = false,
    type: TangemButtonType = TangemButtonType.Accent,
    size: TangemButtonSize = TangemButtonSize.X15,
    shape: TangemButtonShape = TangemButtonShape.Default,
) {
    val statusColor = type.getStatusColor()
    val contentColor = when {
        isEnabled -> TangemTheme.colors2.text.neutral.primaryInvertedConstant
        else -> TangemTheme.colors2.text.status.disabled
    }
    val backgroundModifier = when {
        isEnabled -> Modifier.background(statusColor)
        else -> Modifier.background(TangemTheme.colors2.button.backgroundDisabled)
    }
    TangemButtonInternal(
        onClick = onClick,
        modifier = modifier
            .clip(shape.toShape(size))
            .then(backgroundModifier),
        text = text,
        contentColor = contentColor,
        iconRes = iconRes,
        isEnabled = isEnabled,
        isLoading = isLoading,
        size = size,
        iconPosition = iconPosition,
    )
}

@ReadOnlyComposable
@Composable
private fun TangemButtonType.getStatusColor() = when (this) {
    TangemButtonType.Accent -> TangemTheme.colors2.button.backgroundAccent
    TangemButtonType.Positive -> TangemTheme.colors2.button.backgroundPositive
    else -> TangemTheme.colors2.button.backgroundPrimary
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 480)
@Preview(showBackground = true, widthDp = 480, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun StatusTangemButton_Preview(
    @PreviewParameter(StatusTangemButtonPreviewProvider::class) params: StatusTangemButtonPreviewData,
) {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(TangemTheme.colors2.surface.level1),
        ) {
            params.statuses.fastForEach { (isEnabled, isLoading) ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(21.dp),
                    modifier = Modifier.padding(8.dp),
                ) {
                    repeat(4) { yIndex ->
                        val shape = if (yIndex < 2) TangemButtonShape.Default else TangemButtonShape.Rounded
                        val text = if (yIndex % 2 == 1) null else stringReference("Button")
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            repeat(2) { xIndex ->
                                val iconPosition = if (xIndex == 1) {
                                    TangemButtonIconPosition.Start
                                } else {
                                    TangemButtonIconPosition.End
                                }
                                StatusTangemButton(
                                    onClick = {},
                                    text = text,
                                    size = TangemButtonSize.X15,
                                    shape = shape,
                                    iconPosition = iconPosition,
                                    iconRes = R.drawable.ic_tangem_24,
                                    type = params.type,
                                    isEnabled = isEnabled,
                                    isLoading = isLoading,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class StatusTangemButtonPreviewData(
    val type: TangemButtonType,
    val statuses: List<Pair<Boolean, Boolean>>,
)

private class StatusTangemButtonPreviewProvider : PreviewParameterProvider<StatusTangemButtonPreviewData> {
    val statuses = listOf(
        true to false,
        false to false,
        true to true,
        false to true,
    )
    override val values: Sequence<StatusTangemButtonPreviewData>
        get() = sequenceOf(
            StatusTangemButtonPreviewData(
                type = TangemButtonType.Positive,
                statuses = statuses,
            ),
            StatusTangemButtonPreviewData(
                type = TangemButtonType.Accent,
                statuses = statuses,
            ),
        )
}
// endregion