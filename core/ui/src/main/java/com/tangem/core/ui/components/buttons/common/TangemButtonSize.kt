package com.tangem.core.ui.components.buttons.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class TangemButtonSize {
    Default,
    Text,
    Selector,
    Action,
    RoundedAction,
    WideAction,
    TwoLines,
    Small,
}

@Composable
@ReadOnlyComposable
internal fun TangemButtonSize.toHeightDp(): Dp = when (this) {
    TangemButtonSize.Default, TangemButtonSize.TwoLines -> 48.dp
    TangemButtonSize.Text -> 40.dp
    TangemButtonSize.Selector -> 24.dp
    TangemButtonSize.Action,
    TangemButtonSize.RoundedAction,
    -> 36.dp
    TangemButtonSize.WideAction -> 40.dp
    TangemButtonSize.Small -> 24.dp
}

@Composable
@ReadOnlyComposable
internal fun TangemButtonSize.toIconPadding(): Dp = when (this) {
    TangemButtonSize.Default,
    TangemButtonSize.WideAction,
    TangemButtonSize.TwoLines,
    TangemButtonSize.Small,
    -> 4.dp
    TangemButtonSize.Text -> 8.dp
    TangemButtonSize.Selector -> 0.dp
    TangemButtonSize.Action,
    TangemButtonSize.RoundedAction,
    -> 8.dp
}

@Composable
@ReadOnlyComposable
internal fun TangemButtonSize.toContentPadding(icon: TangemButtonIconPosition): PaddingValues {
    val horizontalPadding = this.toHorizontalContentPadding(icon = icon)

    return when (this) {
        TangemButtonSize.Default -> PaddingValues(
            top = 12.dp,
            bottom = 12.dp,
            start = horizontalPadding.first,
            end = horizontalPadding.second,
        )
        TangemButtonSize.Text -> PaddingValues(
            top = 10.dp,
            bottom = 10.dp,
            start = horizontalPadding.first,
            end = horizontalPadding.second,
        )
        TangemButtonSize.Selector -> PaddingValues(
            top = 0.5.dp,
            bottom = 0.5.dp,
            start = horizontalPadding.first,
            end = horizontalPadding.second,
        )
        TangemButtonSize.Action,
        TangemButtonSize.RoundedAction,
        -> PaddingValues(
            top = 8.dp,
            bottom = 8.dp,
            start = horizontalPadding.first,
            end = horizontalPadding.second,
        )
        TangemButtonSize.WideAction -> PaddingValues(
            top = 10.dp,
            bottom = 10.dp,
            start = horizontalPadding.first,
            end = horizontalPadding.second,
        )
        TangemButtonSize.TwoLines -> PaddingValues(
            top = 6.dp,
            bottom = 6.dp,
            start = horizontalPadding.first,
            end = horizontalPadding.second,
        )
        TangemButtonSize.Small -> PaddingValues(
            top = 4.dp,
            bottom = 4.dp,
            start = horizontalPadding.first,
            end = horizontalPadding.second,
        )
    }
}

@Composable
@ReadOnlyComposable
@Suppress("CyclomaticComplexMethod")
internal fun TangemButtonSize.toHorizontalContentPadding(icon: TangemButtonIconPosition): Pair<Dp, Dp> {
    return when (this) {
        TangemButtonSize.Default,
        TangemButtonSize.WideAction,
        TangemButtonSize.TwoLines,
        -> 16.dp to 16.dp
        TangemButtonSize.Text -> when (icon) {
            is TangemButtonIconPosition.None -> 16.dp to 16.dp
            is TangemButtonIconPosition.Start -> 14.dp to 16.dp
            is TangemButtonIconPosition.End -> 16.dp to 14.dp
        }
        TangemButtonSize.Selector -> 0.5.dp to 0.5.dp
        TangemButtonSize.Action,
        TangemButtonSize.RoundedAction,
        -> when (icon) {
            is TangemButtonIconPosition.None -> 24.dp to 24.dp
            is TangemButtonIconPosition.Start -> 16.dp to 24.dp
            is TangemButtonIconPosition.End -> 24.dp to 16.dp
        }
        TangemButtonSize.Small -> when (icon) {
            is TangemButtonIconPosition.None -> 12.dp to 12.dp
            is TangemButtonIconPosition.Start -> 8.dp to 12.dp
            is TangemButtonIconPosition.End -> 12.dp to 8.dp
        }
    }
}