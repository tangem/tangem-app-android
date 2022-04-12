package com.tangem.tap.common.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
[REDACTED_AUTHOR]
 */
@Composable
fun ErrorView(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current
) {
    Text(
        text,
        color = MaterialTheme.colors.error,
        modifier = modifier,
        style = style
    )
}

@Preview
@Composable
fun ErrorViewTest() {
    Scaffold(
    ) {
        Box(Modifier.padding(16.dp)) {
            ErrorView(text = "Some error description")
        }
    }
}