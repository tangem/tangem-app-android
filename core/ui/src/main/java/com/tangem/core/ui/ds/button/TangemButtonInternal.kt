package com.tangem.core.ui.ds.button

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.conditionalCompose
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.test.BaseButtonTestTags

/**
 * A customizable button component that supports text, icons, and different states.
 *
 * @param onClick       Lambda to be invoked when the button is clicked.
 * @param modifier      Modifier to be applied to the button.
 * @param text          TextReference for the button label.
 * @param iconRes       Drawable resource ID for the icon to be displayed in the button.
 * @param iconPosition  Position of the icon (Start or End).
 * @param enabled       Boolean indicating whether the button is enabled.
 * @param contentColor  Color of the button content (text and icon).
 * @param size          TangemButtonSize defining the size of the button.
 * @param state         TangemButtonState defining the current state of the button.
 *
[REDACTED_AUTHOR]
 */
@Composable
internal fun TangemButtonInternal(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: TextReference? = null,
    @DrawableRes iconRes: Int? = null,
    iconPosition: TangemButtonIconPosition = TangemButtonIconPosition.Start,
    enabled: Boolean = true,
    contentColor: Color = TangemTheme.colors2.text.neutral.primary,
    size: TangemButtonSize = TangemButtonSize.X15,
    state: TangemButtonState = TangemButtonState.Default,
) {
    Row(
        modifier = modifier
            .testTag(BaseButtonTestTags.BUTTON)
            .height(size.toHeightDp())
            .conditionalCompose(text == null) {
                width(size.toHeightDp())
            }
            .clickableSingle(enabled = enabled, onClick = onClick, role = Role.Button)
            .conditionalCompose(text != null) {
                padding(horizontal = size.toPaddingDp())
            }
            .animateContentSize(),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(
            visible = iconRes != null && iconPosition == TangemButtonIconPosition.Start,
            modifier = Modifier.size(size = size.toContentSize()),
        ) {
            val wrappedIconRes = remember(this) { requireNotNull(iconRes) }
            TangemButtonIcon(iconRes = wrappedIconRes, state = state, iconColor = contentColor, size = size)
        }

        AnimatedVisibility(text != null && state != TangemButtonState.Loading) {
            val wrappedText = remember(this) { requireNotNull(text) }
            val textStyle = size.toTextStyle()
            Text(
                text = wrappedText.resolveReference(),
                style = textStyle,
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 12.sp,
                    maxFontSize = textStyle.fontSize,
                ),
                modifier = Modifier.testTag(BaseButtonTestTags.TEXT),
            )
        }

        AnimatedVisibility(
            visible = iconRes != null && iconPosition == TangemButtonIconPosition.End,
            modifier = Modifier.size(size = size.toContentSize()),
        ) {
            val wrappedIconRes = remember(this) { requireNotNull(iconRes) }
            TangemButtonIcon(iconRes = wrappedIconRes, state = state, iconColor = contentColor, size = size)
        }
    }
}

@Composable
private fun TangemButtonIcon(
    @DrawableRes iconRes: Int,
    iconColor: Color,
    state: TangemButtonState,
    size: TangemButtonSize,
) {
    AnimatedContent(state) { targetState ->
        when (targetState) {
            TangemButtonState.Loading -> CircularProgressIndicator(
                color = iconColor,
                strokeWidth = 2.dp,
                strokeCap = StrokeCap.Round,
                modifier = Modifier.padding(
                    when (size) {
                        TangemButtonSize.X7,
                        TangemButtonSize.X8,
                        TangemButtonSize.X9,
                        TangemButtonSize.X10,
                        -> 0.5.dp
                        TangemButtonSize.X12,
                        TangemButtonSize.X15,
                        -> 4.5.dp
                    },
                ),
            )
            else -> Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = iconColor,
            )
        }
    }
}

/**
 * Defines the shape of the Tangem button.
 */
enum class TangemButtonShape {
    Default,
    Rounded,
    ;

    @ReadOnlyComposable
    @Composable
    internal fun toShape(size: TangemButtonSize) = RoundedCornerShape(
        when (this) {
            Default -> size.toShapeRadius()
            Rounded -> 100.dp
        },
    )
}

/**
 * Defines the size of the Tangem button.
 */
enum class TangemButtonSize {
    X7,
    X8,
    X9,
    X10,
    X12,
    X15,
    ;

    @ReadOnlyComposable
    @Composable
    internal fun toHeightDp() = when (this) {
        X7 -> TangemTheme.dimens2.x7
        X8 -> TangemTheme.dimens2.x8
        X9 -> TangemTheme.dimens2.x9
        X10 -> TangemTheme.dimens2.x10
        X12 -> TangemTheme.dimens2.x12
        X15 -> TangemTheme.dimens2.x15
    }

    @ReadOnlyComposable
    @Composable
    internal fun toPaddingDp() = when (this) {
        X7 -> TangemTheme.dimens2.x2
        X8,
        X9,
        X10,
        -> TangemTheme.dimens2.x3
        X12,
        X15,
        -> TangemTheme.dimens2.x6
    }

    @ReadOnlyComposable
    @Composable
    internal fun toContentSize() = when (this) {
        X7,
        X8,
        X9,
        X10,
        -> TangemTheme.dimens2.x5
        X12,
        X15,
        -> TangemTheme.dimens2.x7
    }

    @ReadOnlyComposable
    @Composable
    internal fun toShapeRadius() = when (this) {
        X7,
        X8,
        X9,
        X10,
        -> TangemTheme.dimens2.x2
        X12 -> TangemTheme.dimens2.x3
        X15 -> TangemTheme.dimens2.x4
    }

    @ReadOnlyComposable
    @Composable
    internal fun toTextStyle(): TextStyle = when (this) {
        X7 -> TangemTheme.typography2.bodyRegular14
        X8,
        X9,
        X10,
        X12,
        X15,
        -> TangemTheme.typography2.bodySemibold16
    }
}

/**
 * Defines the state of the Tangem button.
 */
enum class TangemButtonState {
    Default,
    Disabled,
    Pressed,
    Loading,
}

/**
 * Defines the position of the icon in the Tangem button.
 */
enum class TangemButtonIconPosition {
    Start,
    End,
}