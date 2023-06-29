package com.tangem.core.ui.components.buttons.actions

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.R
import com.tangem.core.ui.components.buttons.common.TangemButton
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.components.buttons.common.TangemButtonSize
import com.tangem.core.ui.components.buttons.common.TangemButtonsDefaults
import com.tangem.core.ui.res.TangemTheme

/**
 * [Show in Figma](https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?type=design&node-id=290-305&t=3z98eFnTeyIx5TH5-4)
 */
@Composable
fun RoundedActionButton(config: ActionConfig, modifier: Modifier = Modifier) {
    TangemButton(
        modifier = modifier,
        text = config.text,
        icon = TangemButtonIconPosition.Start(config.iconResId),
        onClick = config.onClick,
        enabled = config.enabled,
        showProgress = false,
        colors = TangemButtonsDefaults.secondaryButtonColors,
        size = TangemButtonSize.RoundedAction,
    )
}

/**
 * [Show in Figma](https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?type=design&node-id=1208-1395&t=3z98eFnTeyIx5TH5-4)
 */
@Composable
fun ActionButton(config: ActionConfig, modifier: Modifier = Modifier) {
    TangemButton(
        modifier = modifier,
        text = config.text,
        icon = TangemButtonIconPosition.Start(config.iconResId),
        onClick = config.onClick,
        enabled = config.enabled,
        showProgress = false,
        colors = TangemButtonsDefaults.secondaryButtonColors,
        size = TangemButtonSize.Action,
    )
}

/**
 * Same as [RoundedActionButton] but colored in primary background color
 */
@Composable
fun BackgroundActionButton(config: ActionConfig, modifier: Modifier = Modifier) {
    TangemButton(
        modifier = modifier,
        text = config.text,
        icon = TangemButtonIconPosition.Start(config.iconResId),
        onClick = config.onClick,
        enabled = config.enabled,
        showProgress = false,
        colors = TangemButtonsDefaults.backgroundButtonColors,
        size = TangemButtonSize.RoundedAction,
    )
}

@Preview(showBackground = true)
@Composable
private fun Preview_ActionButton_Light(@PreviewParameter(ActionStateProvider::class) state: ActionConfig) {
    TangemTheme(isDark = false) {
        Column {
            RoundedActionButton(state)
            ActionButton(state)
            BackgroundActionButton(state)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_ActionButton_Dark(@PreviewParameter(ActionStateProvider::class) state: ActionConfig) {
    TangemTheme {
        Column {
            RoundedActionButton(state)
            ActionButton(state)
            BackgroundActionButton(state)
        }
    }
}

private class ActionStateProvider : CollectionPreviewParameterProvider<ActionConfig>(
    collection = listOf(
        ActionConfig(text = "Send", iconResId = R.drawable.ic_arrow_up_24, onClick = {}),
        ActionConfig(text = "Receive", iconResId = R.drawable.ic_arrow_down_24, enabled = false, onClick = {}),
    ),
)