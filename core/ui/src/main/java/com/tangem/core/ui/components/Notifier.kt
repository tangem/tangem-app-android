package com.tangem.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.res.TangemTheme

/**
 * [Show in Figma](https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?type=design&node-id=68%3A20&mode=design&t=WSV3AxC6zV1y0CHF-1)
 * */
@Composable
fun Notifier(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = TangemTheme.colors.text.primary1,
    backgroundColor: Color = TangemTheme.colors.icon.inactive,
) {
    Box(
        modifier = modifier
            .heightIn(TangemTheme.dimens.size32)
            .background(
                color = backgroundColor,
                shape = TangemTheme.shapes.roundedCorners8,
            ),
    ) {
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = TangemTheme.dimens.size10),
            text = text,
            color = textColor,
            style = TangemTheme.typography.button,
        )
    }
}

@Preview
@Composable
private fun TangemNotifierPreview_Light(@PreviewParameter(NotifierProvider::class) text: String) {
    TangemTheme(isDark = false) {
        Notifier(text = text)
    }
}

@Preview
@Composable
private fun TangemNotifierPreview_Dark(@PreviewParameter(NotifierProvider::class) text: String) {
    TangemTheme(isDark = true) {
        Notifier(text = text)
    }
}

private class NotifierProvider : CollectionPreviewParameterProvider<String>(
    collection = listOf(
        "a",
        "Card 1 of 2",
        "Card 1 of 2 or many other",
    ),
)
