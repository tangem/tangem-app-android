package com.tangem.core.ui.components.inputrow

import android.content.res.Configuration
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

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
    showChevron: Boolean = true,
) {
    InputRowImageBase(
        subtitle = subtitle,
        caption = caption,
        imageUrl = imageUrl,
        modifier = modifier,
        subtitleColor = subtitleColor,
        captionColor = captionColor,
    ) {
        SpacerWMax()
        if (showChevron) {
            Icon(
                painter = painterResource(id = R.drawable.ic_chevron_right_24),
                tint = TangemTheme.colors.icon.informative,
                contentDescription = null,
            )
        }
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun InputRowImageChevron_Preview() {
    TangemThemePreview {
        InputRowImageChevron(
            subtitle = stringReference("Binance"),
            caption = combinedReference(
                resourceReference(R.string.staking_details_apr),
                annotatedReference(
                    buildAnnotatedString {
                        append(" ")
                        withStyle(SpanStyle(TangemTheme.colors.text.accent)) {
                            stringReference("3,54%")
                        }
                    },
                ),
            ),
            imageUrl = "",
        )
    }
}
// endregion