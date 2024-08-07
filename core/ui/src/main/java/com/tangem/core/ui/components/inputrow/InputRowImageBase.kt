package com.tangem.core.ui.components.inputrow

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.components.inputrow.inner.InputRowAsyncImage
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme

@Composable
internal fun InputRowImageBase(
    subtitle: TextReference,
    caption: TextReference?,
    imageUrl: String,
    modifier: Modifier = Modifier,
    subtitleColor: Color = TangemTheme.colors.text.primary1,
    captionColor: Color = TangemTheme.colors.text.tertiary,
    isGrayscaleImage: Boolean = false,
    extraContent: @Composable RowScope.() -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
        modifier = modifier,
    ) {
        InputRowAsyncImage(
            imageUrl = imageUrl,
            isGrayscale = isGrayscaleImage,
            modifier = Modifier
                .size(TangemTheme.dimens.spacing36),
        )
        Column {
            Text(
                text = subtitle.resolveReference(),
                style = TangemTheme.typography.subtitle2,
                color = subtitleColor,
            )
            if (caption != null) {
                Text(
                    text = caption.resolveAnnotatedReference(),
                    style = TangemTheme.typography.caption2,
                    color = captionColor,
                    modifier = Modifier.padding(top = TangemTheme.dimens.spacing2),
                )
            }
        }
        extraContent()
    }
}