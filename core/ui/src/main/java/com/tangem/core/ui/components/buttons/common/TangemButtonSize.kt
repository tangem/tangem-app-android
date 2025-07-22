package com.tangem.core.ui.components.buttons.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme

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
    TangemButtonSize.Default, TangemButtonSize.TwoLines -> TangemTheme.dimens.size48
    TangemButtonSize.Text -> TangemTheme.dimens.size40
    TangemButtonSize.Selector -> TangemTheme.dimens.size24
    TangemButtonSize.Action,
    TangemButtonSize.RoundedAction,
    -> TangemTheme.dimens.size36
    TangemButtonSize.WideAction -> TangemTheme.dimens.size40
    TangemButtonSize.Small -> 24.dp
}

@Composable
@ReadOnlyComposable
internal fun TangemButtonSize.toShape(): Shape = when (this) {
    TangemButtonSize.Default -> TangemTheme.shapes.roundedCornersXMedium
    TangemButtonSize.WideAction -> TangemTheme.shapes.roundedCornersMedium
    TangemButtonSize.Text -> TangemTheme.shapes.roundedCornersSmall
    TangemButtonSize.Selector -> TangemTheme.shapes.roundedCornersSmall
    TangemButtonSize.Action -> TangemTheme.shapes.roundedCornersMedium
    TangemButtonSize.TwoLines -> TangemTheme.shapes.roundedCornersXMedium
    TangemButtonSize.RoundedAction -> TangemTheme.shapes.roundedCornersLarge
    TangemButtonSize.Small -> TangemTheme.shapes.roundedCornersXMedium
}

@Composable
@ReadOnlyComposable
internal fun TangemButtonSize.toIconPadding(): Dp = when (this) {
    TangemButtonSize.Default,
    TangemButtonSize.WideAction,
    TangemButtonSize.TwoLines,
    TangemButtonSize.Small,
    -> TangemTheme.dimens.spacing4
    TangemButtonSize.Text -> TangemTheme.dimens.spacing8
    TangemButtonSize.Selector -> 0.dp
    TangemButtonSize.Action,
    TangemButtonSize.RoundedAction,
    -> TangemTheme.dimens.spacing8
}

@Composable
@ReadOnlyComposable
internal fun TangemButtonSize.toContentPadding(icon: TangemButtonIconPosition): PaddingValues {
    val horizontalPadding = this.toHorizontalContentPadding(icon = icon)

    return when (this) {
        TangemButtonSize.Default -> PaddingValues(
            top = TangemTheme.dimens.spacing12,
            bottom = TangemTheme.dimens.spacing12,
            start = horizontalPadding.first,
            end = horizontalPadding.second,
        )
        TangemButtonSize.Text -> PaddingValues(
            top = TangemTheme.dimens.spacing10,
            bottom = TangemTheme.dimens.spacing10,
            start = horizontalPadding.first,
            end = horizontalPadding.second,
        )
        TangemButtonSize.Selector -> PaddingValues(
            top = TangemTheme.dimens.spacing0_5,
            bottom = TangemTheme.dimens.spacing0_5,
            start = horizontalPadding.first,
            end = horizontalPadding.second,
        )
        TangemButtonSize.Action,
        TangemButtonSize.RoundedAction,
        -> PaddingValues(
            top = TangemTheme.dimens.spacing8,
            bottom = TangemTheme.dimens.spacing8,
            start = horizontalPadding.first,
            end = horizontalPadding.second,
        )
        TangemButtonSize.WideAction -> PaddingValues(
            top = TangemTheme.dimens.spacing10,
            bottom = TangemTheme.dimens.spacing10,
            start = horizontalPadding.first,
            end = horizontalPadding.second,
        )
        TangemButtonSize.TwoLines -> PaddingValues(
            top = TangemTheme.dimens.spacing6,
            bottom = TangemTheme.dimens.spacing6,
            start = horizontalPadding.first,
            end = horizontalPadding.second,
        )
        TangemButtonSize.Small -> PaddingValues(
            top = TangemTheme.dimens.spacing4,
            bottom = TangemTheme.dimens.spacing4,
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
        -> TangemTheme.dimens.spacing16 to TangemTheme.dimens.spacing16
        TangemButtonSize.Text -> when (icon) {
            is TangemButtonIconPosition.None -> TangemTheme.dimens.spacing16 to TangemTheme.dimens.spacing16
            is TangemButtonIconPosition.Start -> TangemTheme.dimens.spacing14 to TangemTheme.dimens.spacing16
            is TangemButtonIconPosition.End -> TangemTheme.dimens.spacing16 to TangemTheme.dimens.spacing14
        }
        TangemButtonSize.Selector -> TangemTheme.dimens.spacing0_5 to TangemTheme.dimens.spacing0_5
        TangemButtonSize.Action,
        TangemButtonSize.RoundedAction,
        -> when (icon) {
            is TangemButtonIconPosition.None -> TangemTheme.dimens.spacing24 to TangemTheme.dimens.spacing24
            is TangemButtonIconPosition.Start -> TangemTheme.dimens.spacing16 to TangemTheme.dimens.spacing24
            is TangemButtonIconPosition.End -> TangemTheme.dimens.spacing24 to TangemTheme.dimens.spacing16
        }
        TangemButtonSize.Small -> when (icon) {
            is TangemButtonIconPosition.None -> TangemTheme.dimens.spacing12 to TangemTheme.dimens.spacing12
            is TangemButtonIconPosition.Start -> TangemTheme.dimens.spacing8 to TangemTheme.dimens.spacing12
            is TangemButtonIconPosition.End -> TangemTheme.dimens.spacing12 to TangemTheme.dimens.spacing8
        }
    }
}