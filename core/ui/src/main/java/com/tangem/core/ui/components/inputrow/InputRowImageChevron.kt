package com.tangem.core.ui.components.inputrow

import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.TangemTheme

/**
 * Input row component with selector
 * [Input Row Image](https://www.figma.com/design/14ISV23YB1yVW1uNVwqrKv/Android?node-id=2100-842&t=hoBXmDX8NeLrp4p6-4)
 *
 * @param subtitle subtitle text
 * @param caption caption text
 * @param imageUrl icon to load
 * @param modifier modifier
 * @param subtitleColor subtitle text color
 * @param captionColor caption text color
 */
@Composable
fun InputRowImageChevron(
    subtitle: TextReference,
    caption: TextReference,
    imageUrl: String,
    modifier: Modifier = Modifier,
    subtitleColor: Color = TangemTheme.colors.text.primary1,
    captionColor: Color = TangemTheme.colors.text.tertiary,
) {
    InputRowImageBase(
        subtitle = subtitle,
        caption = caption,
        imageUrl = imageUrl,
        modifier = modifier,
        subtitleColor = subtitleColor,
        captionColor = captionColor,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_chevron_right_24),
            tint = TangemTheme.colors.icon.informative,
            contentDescription = null,
        )
    }
}