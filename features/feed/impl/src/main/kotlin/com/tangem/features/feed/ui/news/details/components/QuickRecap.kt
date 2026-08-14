package com.tangem.features.feed.ui.news.details.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

@Composable
internal fun QuickRecap(content: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.height(IntrinsicSize.Min)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                imageVector = ImageVector.vectorResource(R.drawable.ic_stars_20),
                contentDescription = null,
            )

            SpacerW(4.dp)

            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle().copy(
                            brush = Brush.linearGradient(
                                GRADIENT_START to Color(LINEAR_GRADIENT_FIRST_PART),
                                GRADIENT_END to Color(LINEAR_GRADIENT_SECOND_PART),
                            ),
                        ),
                    ) {
                        append(stringResourceSafe(R.string.news_quick_recap))
                    }
                },
                style = TangemTheme.typography3.subheading.medium,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }

        SpacerH(10.dp)

        Box {
            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(start = 10.dp),
                thickness = 2.dp,
                color = Color(QUICK_RECAP_DIVIDER_COLOR),
            )
            Text(
                modifier = Modifier.padding(start = 20.dp),
                text = content,
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.primary,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun QuickRecapPreview() {
    TangemThemePreviewRedesign {
        QuickRecap(
            content = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut" +
                " labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris " +
                "nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit " +
                "esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt " +
                "in culpa qui officia deserunt mollit anim id est laborum.",
        )
    }
}

private const val QUICK_RECAP_DIVIDER_COLOR = 0xFFA99FFF
private const val LINEAR_GRADIENT_FIRST_PART = 0xFF7B78FF
private const val LINEAR_GRADIENT_SECOND_PART = 0xFFC56BCD
private const val GRADIENT_START = 0f
private const val GRADIENT_END = 0.5f