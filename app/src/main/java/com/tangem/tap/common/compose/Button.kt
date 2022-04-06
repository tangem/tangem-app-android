package com.tangem.tap.common.compose

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
[REDACTED_AUTHOR]
 */
private class Button {}

@Composable
fun Button(
    text: String = "",
    textId: Int? = null,
    modifier: Modifier = Modifier,
    leftContent: @Composable RowScope.() -> Unit = {},
    rightContent: @Composable RowScope.() -> Unit = {},
    onClick: () -> Unit,
) {
    Button(
        modifier = modifier.height(42.dp),
        onClick = onClick,
    ) {
        leftContent()
        ButtonText(text = textId?.let { stringResource(id = it) } ?: text)
        rightContent()
    }
}

@Composable
fun ButtonText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text,
        modifier = modifier,
        maxLines = 1,
        style = TextStyle(
            fontSize = 16.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    )
}

@Preview
@Composable
fun ButtonTest() {
    Scaffold {
        Button(
            "Some button",
            onClick = {}
        )
    }
}