package com.tangem.core.ui.components

import android.content.res.Configuration
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
import com.tangem.core.ui.res.TangemThemePreview

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
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemNotifierPreview(@PreviewParameter(NotifierProvider::class) text: String) {
    TangemThemePreview {
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