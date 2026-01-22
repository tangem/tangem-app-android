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
 * [Primary Tangem button](https://www.figma.com/design/RU7AIgwHtGdMfy83T5UOoR/Core-Library?node-id=5854-4732&t=euYo1qCxPlQl3Fa6-4)
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
fun PrimaryTangemButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: TextReference? = null,
    @DrawableRes iconRes: Int? = null,
    iconPosition: TangemButtonIconPosition = TangemButtonIconPosition.Start,
    enabled: Boolean = true,
    size: TangemButtonSize = TangemButtonSize.X15,
    state: TangemButtonState = TangemButtonState.Default,
    shape: TangemButtonShape = TangemButtonShape.Default,
) {
    val backgroundModifier = when (state) {
        TangemButtonState.Loading,
        TangemButtonState.Default,
        -> Modifier.background(TangemTheme.colors2.button.backgroundPrimary)
        TangemButtonState.Disabled -> Modifier.background(TangemTheme.colors2.button.backgroundDisabled)
        TangemButtonState.Pressed -> Modifier
            .background(TangemTheme.colors2.button.backgroundPrimary)
            .background(TangemTheme.colors2.overlay.overlaySecondary)
    }
    val contentColor = when (state) {
        TangemButtonState.Disabled -> TangemTheme.colors2.text.status.disabled
        else -> TangemTheme.colors2.text.neutral.primaryInverted
    }
    TangemButtonInternal(
        onClick = onClick,
        modifier = modifier
            .clip(shape.toShape(size))
            .then(backgroundModifier),
        text = text,
        contentColor = contentColor,
        enabled = enabled,
        size = size,
        state = state,
        iconPosition = iconPosition,
        iconRes = iconRes,
    )
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 480)
@Preview(showBackground = true, widthDp = 480, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun PrimaryTangemButton_Preview(
    @PreviewParameter(PrimaryTangemButtonPreviewProvider::class) params: TangemButtonState,
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
                    repeat(TangemButtonIconPosition.entries.size) { xIndex ->
                        PrimaryTangemButton(
                            onClick = {},
                            text = text,
                            size = TangemButtonSize.X15,
                            shape = shape,
                            iconPosition = TangemButtonIconPosition.entries[xIndex],
                            iconRes = R.drawable.ic_tangem_24,
                            state = params,
                        )
                    }
                }
            }
        }
    }
}

private class PrimaryTangemButtonPreviewProvider : PreviewParameterProvider<TangemButtonState> {
    override val values: Sequence<TangemButtonState>
        get() = sequenceOf(
            TangemButtonState.Default,
            TangemButtonState.Pressed,
            TangemButtonState.Loading,
            TangemButtonState.Disabled,
        )
}
// endregion