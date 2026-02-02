package com.tangem.core.ui.ds.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Tangem button that selects the appropriate button style based on the provided [TangemButtonUM].
 * [Button]("https://www.figma.com/design/RU7AIgwHtGdMfy83T5UOoR/Core-Library?node-id=8320-14720&m=dev)
 *
 * @param buttonUM     TangemButtonUM data model containing button properties.
 * @param modifier     Modifier to be applied to the button.
 *
 * @see [TangemButtonInternal] for the underlying implementation.
 */
@Suppress("LongMethod")
@Composable
fun TangemButton(buttonUM: TangemButtonUM, modifier: Modifier = Modifier) {
    when (buttonUM.type) {
        TangemButtonType.Primary -> PrimaryTangemButton(
            onClick = buttonUM.onClick,
            modifier = modifier,
            text = buttonUM.text,
            descriptionText = buttonUM.descriptionText,
            iconRes = buttonUM.iconRes,
            iconPosition = buttonUM.iconPosition,
            enabled = buttonUM.isEnabled,
            size = buttonUM.size,
            state = buttonUM.state,
            shape = buttonUM.shape,
        )
        TangemButtonType.Secondary -> SecondaryTangemButton(
            onClick = buttonUM.onClick,
            modifier = modifier,
            text = buttonUM.text,
            iconRes = buttonUM.iconRes,
            iconPosition = buttonUM.iconPosition,
            enabled = buttonUM.isEnabled,
            size = buttonUM.size,
            state = buttonUM.state,
            shape = buttonUM.shape,
        )
        TangemButtonType.Accent -> AccentTangemButton(
            onClick = buttonUM.onClick,
            modifier = modifier,
            text = buttonUM.text,
            iconRes = buttonUM.iconRes,
            iconPosition = buttonUM.iconPosition,
            enabled = buttonUM.isEnabled,
            size = buttonUM.size,
            state = buttonUM.state,
            shape = buttonUM.shape,
        )
        TangemButtonType.Outline -> OutlineTangemButton(
            onClick = buttonUM.onClick,
            modifier = modifier,
            text = buttonUM.text,
            iconRes = buttonUM.iconRes,
            iconPosition = buttonUM.iconPosition,
            enabled = buttonUM.isEnabled,
            size = buttonUM.size,
            state = buttonUM.state,
            shape = buttonUM.shape,
        )
        TangemButtonType.PrimaryInverse -> PrimaryInverseTangemButton(
            onClick = buttonUM.onClick,
            modifier = modifier,
            text = buttonUM.text,
            iconRes = buttonUM.iconRes,
            iconPosition = buttonUM.iconPosition,
            enabled = buttonUM.isEnabled,
            size = buttonUM.size,
            state = buttonUM.state,
            shape = buttonUM.shape,
        )
        TangemButtonType.Ghost -> GhostTangemButton(
            onClick = buttonUM.onClick,
            modifier = modifier,
            text = buttonUM.text,
            iconRes = buttonUM.iconRes,
            iconPosition = buttonUM.iconPosition,
            enabled = buttonUM.isEnabled,
            size = buttonUM.size,
            state = buttonUM.state,
        )
    }
}