package com.tangem.core.ui.components.fields

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import com.tangem.core.ui.res.TangemTheme

/**
 * A simple input field for a basic dialog with input
 *
 * [Show in Figma] (https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?type=design&node-id=48%3A30&mode=design
 * &t=OsU3Nqsm9d8WDNuv-1)
 */
@Composable
fun SimpleDialogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean = false,
    errorText: String? = null,
    isEnabled: Boolean = true,
    placeholder: String? = null,
) {
    val strokeColor = if (isError) {
        TangemTheme.colors.icon.warning
    } else {
        TangemTheme.colors.stroke.primary
    }
    Column {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TangemTheme.typography.body1,
            cursorBrush = SolidColor(TangemTheme.colors.text.primary1),
            singleLine = true,
            readOnly = !isEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TangemTheme.dimens.spacing24)
                .drawBehind {
                    val strokeWidth = 2f
                    val y = size.height - strokeWidth / 2
                    drawLine(
                        strokeColor,
                        Offset(0f, y),
                        Offset(size.width, y),
                        strokeWidth,
                    )
                },
            decorationBox = { textValue ->
                Box {
                    this@Column.AnimatedVisibility(
                        visible = value.isBlank() && placeholder != null,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        if (value.isBlank() && placeholder != null) {
                            Text(
                                text = placeholder,
                                style = TangemTheme.typography.body1,
                                color = TangemTheme.colors.text.disabled,
                            )
                        }
                    }
                    textValue()
                }
            },
        )
        AnimatedVisibility(
            visible = errorText != null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            if (errorText != null) {
                Text(
                    text = errorText,
                    style = TangemTheme.typography.caption2,
                    color = TangemTheme.colors.text.warning,
                    modifier = Modifier
                        .padding(
                            horizontal = TangemTheme.dimens.spacing24,
                        ),
                )
            }
        }
    }
}