package com.tangem.core.ui.ds.button

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

/**
 * [Ghost Tangem button](https://www.figma.com/design/RU7AIgwHtGdMfy83T5UOoR/Core-Library?node-id=5854-4804)
 *
 * @param buttonUM     TangemButtonUM data model containing button properties.
 * @param modifier     Modifier to be applied to the button.
 */
@Composable
fun GhostTangemButton(buttonUM: TangemButtonUM, modifier: Modifier = Modifier) {
    GhostTangemButton(
        onClick = buttonUM.onClick,
        modifier = modifier,
        text = buttonUM.text,
        iconRes = buttonUM.iconRes,
        iconPosition = buttonUM.iconPosition,
        isEnabled = buttonUM.isEnabled,
        isLoading = buttonUM.isLoading,
        size = buttonUM.size,
        shape = buttonUM.shape,
    )
}

/**
 * [Ghost Tangem button](https://www.figma.com/design/RU7AIgwHtGdMfy83T5UOoR/Core-Library?node-id=5854-4804)
 *
 * @param onClick       Lambda to be invoked when the button is clicked.
 * @param modifier      Modifier to be applied to the button.
 * @param text          TextReference for the button label.
 * @param iconRes       Drawable resource ID for the icon to be displayed in the button.
 * @param iconPosition  Position of the icon (Start or End).
 * @param isEnabled     Boolean indicating whether the button is enabled.
 * @param isLoading     Boolean indicating whether the button is in a loading state.
 * @param size          TangemButtonSize defining the size of the button.
 *
[REDACTED_AUTHOR]
 */
@Composable
fun GhostTangemButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: TextReference? = null,
    @DrawableRes iconRes: Int? = null,
    isEnabled: Boolean = true,
    isLoading: Boolean = false,
    size: TangemButtonSize = TangemButtonSize.X15,
    iconPosition: TangemButtonIconPosition = TangemButtonIconPosition.Start,
    shape: TangemButtonShape = TangemButtonShape.Default,
) {
    val contentColor = if (isEnabled) {
        TangemTheme.colors2.text.neutral.primary
    } else {
        TangemTheme.colors2.text.status.disabled
    }
    TangemButtonInternal(
        onClick = onClick,
        modifier = modifier
            .clip(shape = shape.toShape(size)),
        text = text,
        contentColor = contentColor,
        isEnabled = isEnabled,
        isLoading = isLoading,
        hasPadding = false,
        size = size,
        iconPosition = iconPosition,
        iconRes = iconRes,
    )
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 480)
@Preview(showBackground = true, widthDp = 480, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun GhostTangemButton_Preview(
    @PreviewParameter(GhostTangemButtonPreviewProvider::class) params: Pair<Boolean, Boolean>,
) {
    TangemThemePreviewRedesign {
        val (isEnabled, isLoading) = params
        Row(
            horizontalArrangement = Arrangement.spacedBy(21.dp),
            modifier = Modifier
                .background(TangemTheme.colors2.surface.level1)
                .padding(8.dp),
        ) {
            repeat(4) { yIndex ->
                val text = if (yIndex % 2 == 1) null else stringReference("Button")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(2) { xIndex ->
                        val iconPosition = if (xIndex == 1) {
                            TangemButtonIconPosition.Start
                        } else {
                            TangemButtonIconPosition.End
                        }
                        GhostTangemButton(
                            onClick = {},
                            text = text,
                            size = TangemButtonSize.X15,
                            iconPosition = iconPosition,
                            iconRes = R.drawable.ic_tangem_24,
                            isEnabled = isEnabled,
                            isLoading = isLoading,
                        )
                    }
                }
            }
        }
    }
}

private class GhostTangemButtonPreviewProvider : PreviewParameterProvider<Pair<Boolean, Boolean>> {
    override val values: Sequence<Pair<Boolean, Boolean>>
        get() = sequenceOf(
            true to false,
            false to false,
            true to true,
            false to true,
        )
}
// endregion