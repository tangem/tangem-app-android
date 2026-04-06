package com.tangem.core.ui.ds.button

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.test.BaseButtonTestTags

private const val LOADING_ANIMATION_DURATION = 150

/**
 * A customizable button component that supports text, icons, and different states.
 *
 * @param onClick       Lambda to be invoked when the button is clicked.
 * @param modifier      Modifier to be applied to the button.
 * @param text          TextReference for the button label.
 * @param iconRes       Drawable resource ID for the icon to be displayed in the button.
 * @param iconPosition  Position of the icon (Start or End).
 * @param isEnabled     Boolean indicating whether the button is enabled.
 * @param isLoading     Boolean indicating whether the button is in a loading state.
 * @param hasPadding    Boolean indicating whether the button should have padding around its content.
 * @param contentColor  Color of the button content (text and icon).
 * @param size          TangemButtonSize defining the size of the button.
 *
[REDACTED_AUTHOR]
 */
@Composable
internal fun TangemButtonInternal(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: TextReference? = null,
    descriptionText: TextReference? = null,
    @DrawableRes iconRes: Int? = null,
    iconPosition: TangemButtonIconPosition = TangemButtonIconPosition.Start,
    isEnabled: Boolean = true,
    isLoading: Boolean = false,
    hasPadding: Boolean = true,
    contentColor: Color = TangemTheme.colors2.text.neutral.primary,
    size: TangemButtonSize = TangemButtonSize.X15,
) {
    ProvideButtonRippleConfiguration {
        Box(
            modifier = modifier
                .testTag(BaseButtonTestTags.BUTTON)
                .clickableSingle(enabled = isEnabled, onClick = onClick, role = Role.Button)
                .heightIn(min = size.toHeightDp())
                .conditionalCompose(text == null) {
                    width(size.toHeightDp())
                }
                .conditionalCompose(text != null && hasPadding) {
                    padding(size.toPaddingDp())
                }
                .animateContentSize(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.Center)
                    .conditional(isLoading) {
                        alpha(0f)
                    },
            ) {
                val isStartIcon = iconRes != null && iconPosition == TangemButtonIconPosition.Start
                TangemButtonIcon(
                    iconRes = iconRes,
                    iconColor = contentColor,
                    isVisible = isStartIcon,
                    size = size,
                )
                if (isStartIcon && (text != null || descriptionText != null)) {
                    SpacerW(TangemTheme.dimens2.x1)
                }

                ButtonContent(
                    text = text,
                    descriptionText = descriptionText,
                    contentColor = contentColor,
                    size = size,
                )

                val isEndIcon = iconRes != null && iconPosition == TangemButtonIconPosition.End
                if (isEndIcon && (text != null || descriptionText != null)) {
                    SpacerW(TangemTheme.dimens2.x1)
                }
                TangemButtonIcon(
                    iconRes = iconRes,
                    iconColor = contentColor,
                    isVisible = isEndIcon,
                    size = size,
                )
            }
            AnimatedVisibility(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(size.toContentSize()),
                visible = isLoading,
                exit = fadeOut(animationSpec = tween(LOADING_ANIMATION_DURATION)),
                enter = fadeIn(animationSpec = tween(LOADING_ANIMATION_DURATION)),
            ) {
                CircularProgressIndicator(
                    color = contentColor,
                    strokeWidth = 2.dp,
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier.size(size.toContentSize()),
                )
            }
        }
    }
}

@Composable
private fun ButtonContent(
    text: TextReference?,
    descriptionText: TextReference?,
    contentColor: Color,
    size: TangemButtonSize,
) {
    Column {
        AnimatedVisibility(text != null) {
            val wrappedText = remember(this) { text.orEmpty() }
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
        AnimatedVisibility(descriptionText != null) {
            val wrappedText = remember(this) { descriptionText.orEmpty() }
            val textStyle = TangemTheme.typography2.captionSemibold12
            Text(
                text = wrappedText.resolveReference(),
                style = textStyle,
                color = TangemTheme.colors2.text.status.disabled,
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
    }
}

@Composable
private inline fun ProvideButtonRippleConfiguration(crossinline content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalRippleConfiguration provides RippleConfiguration(
            color = TangemTheme.colors2.overlay.overlaySecondary,
            RippleAlpha(
                pressedAlpha = 0.4f,
                focusedAlpha = 0.4f,
                draggedAlpha = 0.4f,
                hoveredAlpha = 0.4f,
            ),
        ),
    ) {
        content()
    }
}

@Composable
private fun TangemButtonIcon(
    @DrawableRes iconRes: Int?,
    iconColor: Color,
    isVisible: Boolean,
    size: TangemButtonSize,
) {
    AnimatedVisibility(
        visible = isVisible,
        modifier = Modifier.size(size = size.toContentSize()),
    ) {
        val wrappedIconRes = remember(iconRes) { requireNotNull(iconRes) }
        Icon(
            painter = painterResource(id = wrappedIconRes),
            contentDescription = null,
            tint = iconColor,
        )
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
        X7 -> PaddingValues(
            horizontal = TangemTheme.dimens2.x2,
            vertical = TangemTheme.dimens2.x0_5,
        )
        X8 -> PaddingValues(
            horizontal = TangemTheme.dimens2.x3,
            vertical = TangemTheme.dimens2.x1_5,
        )
        X9 -> PaddingValues(
            horizontal = TangemTheme.dimens2.x3,
            vertical = TangemTheme.dimens2.x2,
        )
        X10 -> PaddingValues(
            horizontal = TangemTheme.dimens2.x3,
            vertical = TangemTheme.dimens2.x2_5,
        )
        X12 -> PaddingValues(
            horizontal = TangemTheme.dimens2.x6,
            vertical = TangemTheme.dimens2.x2_5,
        )
        X15 -> PaddingValues(
            horizontal = TangemTheme.dimens2.x6,
            vertical = TangemTheme.dimens2.x4,
        )
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
 * Defines the position of the icon in the Tangem button.
 */
enum class TangemButtonIconPosition {
    Start,
    End,
}