package com.tangem.tap.common.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tangem.core.ui.components.SpacerH8
import com.tangem.tap.common.compose.extensions.stringResourceDefault
import com.tangem.wallet.R

/**
* [REDACTED_AUTHOR]
 */
@Composable
fun RectangleButton(
    modifier: Modifier = Modifier,
    text: String = "",
    textId: Int? = null,
    isEnabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    leadingView: @Composable RowScope.() -> Unit = {},
    middleView: @Composable RowScope.() -> Unit = { TextInButton(text = text, textId = textId) },
    trailingView: @Composable RowScope.() -> Unit = {},
    onClick: () -> Unit,
) {
    Button(
        modifier = modifier,
        contentPadding = contentPadding,
        enabled = isEnabled,
        onClick = onClick,
    ) {
        leadingView()
        middleView()
        trailingView()
    }
}

@Composable
private fun TextInButton(
    modifier: Modifier = Modifier,
    text: String = "",
    textId: Int? = null,
) {
    Text(
        modifier = modifier,
        text = stringResourceDefault(textId, text),
        maxLines = 1,
        style = TextStyle(
            fontSize = 16.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
        )
    )
}

@Composable
fun PasteButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    dpSize: DpSize = DpSize(40.dp, 40.dp),
    onClick: () -> Unit,
    tint: Color? = null,
    content: @Composable (() -> Unit)? = null
) {
    IconButton(
        modifier = modifier.size(dpSize),
        enabled = enabled,
        onClick = onClick,
    ) {
        when (content) {
            null -> {
                val tintColor = tint
                    ?: colorResource(id = if (enabled) R.color.accent else R.color.accent_disabled)
                Icon(
                    painterResource(id = R.drawable.ic_paste),
                    contentDescription = "Paste",
                    tint = tintColor,
                )
            }
            else -> content()
        }
    }
}

@Composable
fun ClearButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    dpSize: DpSize = DpSize(40.dp, 40.dp),
    onClick: () -> Unit,
    tint: Color? = null,
    content: @Composable (() -> Unit)? = null
) {
    IconButton(
        modifier = modifier.size(dpSize),
        enabled = enabled,
        onClick = onClick,
    ) {
        when (content) {
            null -> {
                val tintColor = tint
                    ?: colorResource(id = if (enabled) R.color.accent else R.color.accent_disabled)
                Icon(
                    painterResource(id = R.drawable.ic_clear),
                    contentDescription = "Clear",
                    tint = tintColor,
                )
            }
            else -> content()
        }
    }
}

@Preview
@Composable
fun ButtonTest() {
    Scaffold {
        Column(modifier = Modifier.padding(16.dp)) {
            PreviewItem("Button") {
                RectangleButton(text = "Some button") {}
            }
            PreviewItem("PasteButton") {
                PasteButton(onClick = {})
            }
        }
    }
}

@Composable
fun PreviewItem(
    name: String,
    content: @Composable RowScope.() -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            modifier = Modifier.weight(1f),
            text = name,
        )
        content()
    }
    SpacerH8()
}
