package com.tangem.tap.common.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.res.TangemTheme

// Copy from com.tangem.core.ui.components
// TODO: Delete this after fields has been moved to core-ui module
internal object TangemTextFieldsDefault {
    val defaultTextFieldColors: TangemTextFieldColors
        @Composable @Stable get() = TangemTextFieldColors(
            textColor = TangemTheme.colors.text.primary1,
            disabledTextColor = TangemTheme.colors.text.disabled,
            backgroundColor = Color.Transparent,
            cursorColor = TangemTheme.colors.icon.primary1,
            errorCursorColor = TangemTheme.colors.icon.warning,
            focusedIndicatorColor = TangemTheme.colors.icon.primary1,
            unfocusedIndicatorColor = TangemTheme.colors.stroke.primary,
            disabledIndicatorColor = TangemTheme.colors.stroke.primary,
            errorIndicatorColor = TangemTheme.colors.icon.warning,
            leadingIconColor = TangemTheme.colors.icon.informative,
            disabledLeadingIconColor = Color.Transparent,
            errorLeadingIconColor = TangemTheme.colors.icon.warning,
            trailingIconColor = TangemTheme.colors.icon.informative,
            disabledTrailingIconColor = Color.Transparent,
            errorTrailingIconColor = TangemTheme.colors.icon.warning,
            focusedLabelColor = TangemTheme.colors.text.primary1,
            unfocusedLabelColor = TangemTheme.colors.text.secondary,
            disabledLabelColor = TangemTheme.colors.text.disabled,
            errorLabelColor = TangemTheme.colors.icon.warning,
            placeholderColor = TangemTheme.colors.text.secondary,
            disabledPlaceholderColor = TangemTheme.colors.text.disabled,
            captionColor = TangemTheme.colors.text.tertiary,
            disabledCaptionColor = TangemTheme.colors.text.disabled,
            errorCaptionColor = TangemTheme.colors.icon.warning,
        )
}

@Immutable
internal data class TangemTextFieldColors(
    private val textColor: Color,
    private val disabledTextColor: Color,
    private val cursorColor: Color,
    private val errorCursorColor: Color,
    private val focusedIndicatorColor: Color,
    private val unfocusedIndicatorColor: Color,
    private val errorIndicatorColor: Color,
    private val disabledIndicatorColor: Color,
    private val leadingIconColor: Color,
    private val disabledLeadingIconColor: Color,
    private val errorLeadingIconColor: Color,
    private val trailingIconColor: Color,
    private val disabledTrailingIconColor: Color,
    private val errorTrailingIconColor: Color,
    private val backgroundColor: Color,
    private val focusedLabelColor: Color,
    private val unfocusedLabelColor: Color,
    private val disabledLabelColor: Color,
    private val errorLabelColor: Color,
    private val placeholderColor: Color,
    private val disabledPlaceholderColor: Color,
    private val captionColor: Color,
    private val disabledCaptionColor: Color,
    private val errorCaptionColor: Color,
) : TextFieldColors {

    @Composable
    override fun leadingIconColor(enabled: Boolean, isError: Boolean): State<Color> {
        return rememberUpdatedState(
            when {
                !enabled -> disabledLeadingIconColor
                isError -> errorLeadingIconColor
                else -> leadingIconColor
            },
        )
    }

    @Composable
    override fun trailingIconColor(enabled: Boolean, isError: Boolean): State<Color> {
        return rememberUpdatedState(
            when {
                !enabled -> disabledTrailingIconColor
                isError -> errorTrailingIconColor
                else -> trailingIconColor
            },
        )
    }

    @Composable
    override fun indicatorColor(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource,
    ): State<Color> {
        val focused by interactionSource.collectIsFocusedAsState()

        val targetValue = when {
            !enabled -> disabledIndicatorColor
            isError -> errorIndicatorColor
            focused -> focusedIndicatorColor
            else -> unfocusedIndicatorColor
        }
        return if (enabled) {
            animateColorAsState(targetValue, tween(durationMillis = 120))
        } else {
            rememberUpdatedState(targetValue)
        }
    }

    @Composable
    override fun backgroundColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(backgroundColor)
    }

    @Composable
    override fun placeholderColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) placeholderColor else disabledPlaceholderColor)
    }

    @Composable
    override fun labelColor(enabled: Boolean, error: Boolean, interactionSource: InteractionSource): State<Color> {
        val focused by interactionSource.collectIsFocusedAsState()

        val targetValue = when {
            !enabled -> disabledLabelColor
            error -> errorLabelColor
            focused -> focusedLabelColor
            else -> unfocusedLabelColor
        }
        return rememberUpdatedState(targetValue)
    }

    @Composable
    override fun textColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) textColor else disabledTextColor)
    }

    @Composable
    override fun cursorColor(isError: Boolean): State<Color> {
        return rememberUpdatedState(if (isError) errorCursorColor else cursorColor)
    }
}