package com.tangem.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.res.ButtonColorType
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TextColorType
import com.tangem.core.ui.res.buttonColor
import com.tangem.core.ui.res.textColor

@Preview(widthDp = 328, heightDp = 48, showBackground = true)
@Composable
fun Preview_PrimaryStartIconButton_Enabled_InLightTheme() {
    TangemTheme(isDarkTheme = false) {
        PrimaryStartIconButton(text = "Manage tokens", iconResId = R.drawable.ic_tangem, enabled = true, onClicked = {})
    }
}

@Preview(widthDp = 328, heightDp = 48, showBackground = true)
@Composable
fun Preview_PrimaryStartIconButton_Enabled_InDarkTheme() {
    TangemTheme(isDarkTheme = true) {
        PrimaryStartIconButton(text = "Manage tokens", iconResId = R.drawable.ic_tangem, enabled = true, onClicked = {})
    }
}

@Preview(widthDp = 328, heightDp = 48, showBackground = true)
@Composable
fun Preview_PrimaryStartIconButton_Disabled_InLightTheme() {
    TangemTheme(isDarkTheme = false) {
        PrimaryStartIconButton(
            text = "Manage tokens",
            iconResId = R.drawable.ic_tangem,
            enabled = false,
            onClicked = {},
        )
    }
}

@Preview(widthDp = 328, heightDp = 48, showBackground = true)
@Composable
fun Preview_PrimaryStartIconButton_Disabled_InDarkTheme() {
    TangemTheme(isDarkTheme = true) {
        PrimaryStartIconButton(
            text = "Manage tokens",
            iconResId = R.drawable.ic_tangem,
            enabled = false,
            onClicked = {},
        )
    }
}

@Preview(widthDp = 328, heightDp = 48, showBackground = true)
@Composable
fun Preview_PrimaryEndIconButton_Enabled_InLightTheme() {
    TangemTheme(isDarkTheme = false) {
        PrimaryEndIconButton(text = "Manage tokens", iconResId = R.drawable.ic_tangem, enabled = true, onClicked = {})
    }
}

@Preview(widthDp = 328, heightDp = 48, showBackground = true)
@Composable
fun Preview_PrimaryEndIconButton_Enabled_InDarkTheme() {
    TangemTheme(isDarkTheme = true) {
        PrimaryEndIconButton(text = "Manage tokens", iconResId = R.drawable.ic_tangem, enabled = true, onClicked = {})
    }
}

@Preview(widthDp = 328, heightDp = 48, showBackground = true)
@Composable
fun Preview_PrimaryEndIconButton_Disabled_InLightTheme() {
    TangemTheme(isDarkTheme = false) {
        PrimaryEndIconButton(text = "Manage tokens", iconResId = R.drawable.ic_tangem, enabled = false, onClicked = {})
    }
}

@Preview(widthDp = 328, heightDp = 48, showBackground = true)
@Composable
fun Preview_PrimaryEndIconButton_Disabled_InDarkTheme() {
    TangemTheme(isDarkTheme = true) {
        PrimaryEndIconButton(text = "Manage tokens", iconResId = R.drawable.ic_tangem, enabled = false, onClicked = {})
    }
}

/**
 * Primary button with an icon at the beginning of the layout
 *
 * @param text      button text
 * @param iconResId button icon res id
 * @param enabled   controls the enabled state of the button
 * @param onClicked the lambda to be invoked when this button is pressed
 *
 * @see <a href = "https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?node-id=233%3A258&t=WdN5XpixzZLlQAZO-4"
 * >Figma component</a>
 */
@Composable
fun PrimaryStartIconButton(text: String, @DrawableRes iconResId: Int, enabled: Boolean = true, onClicked: () -> Unit) {
    PrimaryButtonRow(enabled = enabled, onClicked = onClicked) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing8)))
        Text(text = text)
    }
}

/**
 * Primary button with an icon at the end of the layout
 *
 * @param text      button text
 * @param iconResId button icon res id
 * @param enabled   controls the enabled state of the button
 * @param onClicked the lambda to be invoked when this button is pressed
 *
 * @see <a href = "https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?node-id=68%3A47&t=WdN5XpixzZLlQAZO-4"
 * >Figma component</a>
 */
@Composable
fun PrimaryEndIconButton(text: String, @DrawableRes iconResId: Int, enabled: Boolean = true, onClicked: () -> Unit) {
    PrimaryButtonRow(enabled = enabled, onClicked = onClicked) {
        Text(text = text)
        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing8)))
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = null,
        )
    }
}

@Composable
private fun PrimaryButtonRow(enabled: Boolean, onClicked: () -> Unit, content: @Composable (RowScope.() -> Unit)) {
    Button(
        onClick = onClicked,
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.size48)),
        enabled = enabled,
        shape = RoundedCornerShape(dimensionResource(id = R.dimen.radius12)),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.buttonColor(type = ButtonColorType.PRIMARY),
            contentColor = MaterialTheme.colors.textColor(type = TextColorType.PRIMARY2),
            disabledBackgroundColor = MaterialTheme.colors.buttonColor(type = ButtonColorType.DISABLED),
            disabledContentColor = MaterialTheme.colors.textColor(type = TextColorType.DISABLED),
        ),
        content = content,
    )
}