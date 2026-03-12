package com.tangem.core.ui.ds.button

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
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
        enabled = buttonUM.isEnabled,
        type = buttonUM.type,
        size = buttonUM.size,
        state = buttonUM.state,
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
 * @param enabled       Boolean indicating whether the button is enabled.
 * @param size          TangemButtonSize defining the size of the button.
 * @param state         TangemButtonState defining the current state of the button.
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
    enabled: Boolean = true,
    type: TangemButtonType = TangemButtonType.Accent,
    size: TangemButtonSize = TangemButtonSize.X15,
    state: TangemButtonState = TangemButtonState.Default,
    shape: TangemButtonShape = TangemButtonShape.Default,
) {
    val statusColor = type.getStatusColor()
    val contentColor = when (state) {
        TangemButtonState.Disabled -> TangemTheme.colors2.text.status.disabled
        else -> TangemTheme.colors2.text.neutral.primaryInvertedConstant
    }
    val backgroundModifier = when (state) {
        TangemButtonState.Disabled -> Modifier.background(TangemTheme.colors2.button.backgroundDisabled)
        TangemButtonState.Default -> Modifier.background(statusColor)
        TangemButtonState.Loading,
        TangemButtonState.Pressed,
        -> Modifier
            .background(statusColor)
            .background(TangemTheme.colors2.overlay.overlaySecondary)
    }
    TangemButtonInternal(
        onClick = onClick,
        modifier = modifier
            .clip(shape.toShape(size))
            .then(backgroundModifier),
        text = text,
        contentColor = contentColor,
        iconRes = iconRes,
        enabled = enabled,
        size = size,
        state = state,
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
    @PreviewParameter(AccentTangemButtonPreviewProvider::class) params: TangemButtonState,
) {
    TangemThemePreviewRedesign {
        Row(
            horizontalArrangement = Arrangement.spacedBy(21.dp),
            modifier = Modifier
                .background(TangemTheme.colors2.surface.level1)
                .padding(8.dp),
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
                            type = TangemButtonType.Accent,
                            state = params,
                        )
                    }
                }
            }
        }
    }
}

private class AccentTangemButtonPreviewProvider : PreviewParameterProvider<TangemButtonState> {
    override val values: Sequence<TangemButtonState>
        get() = sequenceOf(
            TangemButtonState.Default,
            TangemButtonState.Pressed,
            TangemButtonState.Loading,
            TangemButtonState.Disabled,
        )
}
// endregion