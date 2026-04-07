package com.tangem.core.ui.ds.button

import android.content.res.Configuration
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
import com.tangem.core.ui.components.haze.hazeEffectTangem
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

/**
 * [Secondary Tangem button](https://www.figma.com/design/RU7AIgwHtGdMfy83T5UOoR/Core-Library?node-id=5854-4796)
 *
 * @param buttonUM     TangemButtonUM data model containing button properties.
 * @param modifier     Modifier to be applied to the button.
 */
@Composable
fun SecondaryTangemButton(buttonUM: TangemButtonUM, modifier: Modifier = Modifier) {
    SecondaryTangemButton(
        onClick = buttonUM.onClick,
        modifier = modifier,
        text = buttonUM.text,
        tangemIconUM = buttonUM.tangemIconUM,
        iconPosition = buttonUM.iconPosition,
        isEnabled = buttonUM.isEnabled,
        isLoading = buttonUM.isLoading,
        size = buttonUM.size,
        shape = buttonUM.shape,
    )
}

/**
 * [Secondary Tangem button](https://www.figma.com/design/RU7AIgwHtGdMfy83T5UOoR/Core-Library?node-id=5854-4796)
 *
 * @param onClick       Lambda to be invoked when the button is clicked.
 * @param modifier      Modifier to be applied to the button.
 * @param text          TextReference for the button label.
 * @param tangemIconUM  Icon for the icon to be displayed in the button.
 * @param iconPosition  Position of the icon (Start or End).
 * @param isEnabled     Boolean indicating whether the button is enabled.
 * @param isLoading     Boolean indicating whether the button is in a loading state.
 * @param size          TangemButtonSize defining the size of the button.
 * @param shape         TangemButtonShape defining the shape of the button.
 *
[REDACTED_AUTHOR]
 */
@Composable
fun SecondaryTangemButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: TextReference? = null,
    tangemIconUM: TangemIconUM? = null,
    iconPosition: TangemButtonIconPosition = TangemButtonIconPosition.Start,
    isEnabled: Boolean = true,
    isLoading: Boolean = false,
    size: TangemButtonSize = TangemButtonSize.X15,
    shape: TangemButtonShape = TangemButtonShape.Default,
) {
    val backgroundModifier = if (isEnabled) {
        Modifier.background(TangemTheme.colors2.button.backgroundSecondary)
    } else {
        Modifier.background(TangemTheme.colors2.button.backgroundDisabled)
    }
    val contentColor = if (isEnabled) {
        TangemTheme.colors2.text.neutral.primary
    } else {
        TangemTheme.colors2.text.status.disabled
    }

    TangemButtonInternal(
        onClick = onClick,
        modifier = modifier
            .clip(shape.toShape(size))
            .hazeEffectTangem()
            .then(backgroundModifier),
        text = text,
        contentColor = contentColor,
        tangemIconUM = tangemIconUM,
        isEnabled = isEnabled,
        isLoading = isLoading,
        size = size,
        iconPosition = iconPosition,
    )
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 480)
@Preview(showBackground = true, widthDp = 480, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun SecondaryTangemButton_Preview(
    @PreviewParameter(SecondaryTangemButtonPreviewProvider::class) params: Pair<Boolean, Boolean>,
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
                val shape = if (yIndex < 2) TangemButtonShape.Default else TangemButtonShape.Rounded
                val text = if (yIndex % 2 == 1) null else stringReference("Button")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(2) { xIndex ->
                        val iconPosition = if (xIndex == 0) {
                            TangemButtonIconPosition.Start
                        } else {
                            TangemButtonIconPosition.End
                        }
                        SecondaryTangemButton(
                            onClick = {},
                            text = text,
                            size = TangemButtonSize.X15,
                            shape = shape,
                            iconPosition = iconPosition,
                            tangemIconUM = TangemIconUM.Icon(
                                iconRes = R.drawable.ic_tangem_24,
                                tintReference = {
                                    if (isEnabled) {
                                        TangemTheme.colors2.graphic.neutral.primary
                                    } else {
                                        TangemTheme.colors2.graphic.neutral.quaternary
                                    }
                                },
                            ),
                            isEnabled = isEnabled,
                            isLoading = isLoading,
                        )
                    }
                }
            }
        }
    }
}

private class SecondaryTangemButtonPreviewProvider : PreviewParameterProvider<Pair<Boolean, Boolean>> {
    override val values: Sequence<Pair<Boolean, Boolean>>
        get() = sequenceOf(
            true to false,
            false to false,
            true to true,
            false to true,
        )
}
// endregion